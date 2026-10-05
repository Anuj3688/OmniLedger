package dev.fintech.omniledger.service.ingestion;

import dev.fintech.omniledger.exception.AccountNotFoundException;
import dev.fintech.omniledger.exception.CurrencyMismatchException;
import dev.fintech.omniledger.exception.InsufficientBalanceException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.JournalEntry;
import dev.fintech.omniledger.model.PostingLine;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PostingType;
import dev.fintech.omniledger.model.enums.RejectionCode;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileTransactionPostingProcessor {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PostingResult postTransactionGroup(
            String sourceSystem,
            String referenceId,
            List<ParsedPostingLine> lines,
            Map<String, UUID> resolvedAccounts
    ) {
        String idempotencyKey = sourceSystem + ":" + referenceId;

        if (journalEntryRepository.existsByIdempotencyKey(idempotencyKey)) {
            return PostingResult.failure(RejectionCode.DUPLICATE_REFERENCE,
                    "Transaction reference already posted: " + idempotencyKey);
        }

        if (lines == null || lines.size() < 2) {
            return PostingResult.failure(RejectionCode.UNBALANCED_ENTRY,
                    "Paired transactions require at least 2 posting legs (found " + (lines == null ? 0 : lines.size()) + ")");
        }

        // Validate all accounts are resolved
        for (ParsedPostingLine line : lines) {
            UUID accountId = resolvedAccounts.get(line.externalAccountId());
            if (accountId == null) {
                return PostingResult.failure(RejectionCode.UNKNOWN_ACCOUNT,
                        "Unknown external account: " + line.externalAccountId());
            }
        }

        // Validate currencies are identical across all legs
        Currency baseCurrency = lines.get(0).currency();
        for (ParsedPostingLine line : lines) {
            if (line.currency() != baseCurrency) {
                return PostingResult.failure(RejectionCode.CURRENCY_MISMATCH,
                        "Currency mismatch within transaction reference: " + line.currency() + " vs " + baseCurrency);
            }
        }

        // Validate double-entry balance: sum(debit) == sum(credit)
        BigDecimal debitSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;
        for (ParsedPostingLine line : lines) {
            if (line.direction() == PostingType.DEBIT) {
                debitSum = debitSum.add(line.amount());
            } else if (line.direction() == PostingType.CREDIT) {
                creditSum = creditSum.add(line.amount());
            } else {
                return PostingResult.failure(RejectionCode.MALFORMED_RECORD,
                        "Invalid direction: " + line.direction());
            }
        }

        if (debitSum.compareTo(creditSum) != 0) {
            return PostingResult.failure(RejectionCode.UNBALANCED_ENTRY,
                    String.format("Unbalanced entry: total DEBIT (%s) != total CREDIT (%s)", debitSum, creditSum));
        }

        // Lock accounts deterministically by sorted UUID to eliminate deadlocks
        List<UUID> distinctAccountIds = lines.stream()
                .map(l -> resolvedAccounts.get(l.externalAccountId()))
                .distinct()
                .sorted()
                .toList();

        Map<UUID, Account> lockedAccounts = new HashMap<>();
        for (UUID accountId : distinctAccountIds) {
            Account account = accountRepository.findByIdWithLock(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
            if (account.getCurrency() != baseCurrency) {
                return PostingResult.failure(RejectionCode.CURRENCY_MISMATCH,
                        String.format("Account %s currency %s does not match line currency %s",
                                accountId, account.getCurrency(), baseCurrency));
            }
            lockedAccounts.put(accountId, account);
        }

        // Execute debits and credits on accounts
        try {
            for (ParsedPostingLine line : lines) {
                UUID accountId = resolvedAccounts.get(line.externalAccountId());
                Account account = lockedAccounts.get(accountId);
                if (line.direction() == PostingType.DEBIT) {
                    account.debit(line.amount());
                } else {
                    account.credit(line.amount());
                }
            }
        } catch (InsufficientBalanceException ex) {
            log.warn("Insufficient balance for posting reference={}: {}", referenceId, ex.getMessage());
            return PostingResult.failure(RejectionCode.INSUFFICIENT_BALANCE, ex.getMessage());
        }

        // Construct immutable JournalEntry with PostingLines
        String description = lines.get(0).description() != null ? lines.get(0).description() : "File ingestion: " + referenceId;
        JournalEntry journalEntry = JournalEntry.builder()
                .idempotencyKey(idempotencyKey)
                .currency(baseCurrency)
                .description(description)
                .build();

        for (ParsedPostingLine line : lines) {
            UUID accountId = resolvedAccounts.get(line.externalAccountId());
            Account account = lockedAccounts.get(accountId);
            PostingLine postingLine = PostingLine.builder()
                    .account(account)
                    .amount(line.amount())
                    .direction(line.direction())
                    .build();
            journalEntry.addPostingLine(postingLine);
        }

        accountRepository.saveAll(lockedAccounts.values());
        JournalEntry savedEntry = journalEntryRepository.save(journalEntry);

        log.debug("Successfully posted transaction reference={} with journalEntryId={}", referenceId, savedEntry.getId());
        return PostingResult.success(savedEntry);
    }
}
