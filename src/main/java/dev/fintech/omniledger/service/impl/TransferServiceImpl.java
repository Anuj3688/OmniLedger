package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.request.TransferRequest;
import dev.fintech.omniledger.dto.response.PostingLineResponse;
import dev.fintech.omniledger.dto.response.TransferResponse;
import dev.fintech.omniledger.exception.AccountNotFoundException;
import dev.fintech.omniledger.exception.CurrencyMismatchException;
import dev.fintech.omniledger.exception.DuplicateIdempotencyKeyException;
import dev.fintech.omniledger.exception.InsufficientBalanceException;
import dev.fintech.omniledger.exception.LedgerContentionException;
import dev.fintech.omniledger.exception.SameAccountTransferException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.JournalEntry;
import dev.fintech.omniledger.model.PostingLine;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.PostingType;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.JournalEntryRepository;
import dev.fintech.omniledger.service.EventService;
import dev.fintech.omniledger.service.TransferService;
import dev.fintech.omniledger.service.routing.AccountShardRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final EventService eventService;
    private final AccountShardRouter accountShardRouter;

    @Override
    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    PessimisticLockingFailureException.class,
                    ConcurrencyFailureException.class,
                    TransientDataAccessException.class
            },
            noRetryFor = {
                    InsufficientBalanceException.class,
                    CurrencyMismatchException.class,
                    SameAccountTransferException.class,
                    DuplicateIdempotencyKeyException.class,
                    AccountNotFoundException.class,
                    IllegalArgumentException.class
            },
            maxAttempts = 4,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000, random = true)
    )
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("Received transfer request: idempotencyKey={}, from={}, to={}, amount={}, currency={}",
                request != null ? request.idempotencyKey() : null,
                request != null ? request.sourceAccountId() : null,
                request != null ? request.destinationAccountId() : null,
                request != null ? request.amount() : null,
                request != null ? request.currency() : null);

        validateTransferRequest(request);

        // Idempotency check for safe retries
        Optional<JournalEntry> existingEntryOpt = journalEntryRepository.findByIdempotencyKeyWithLines(request.idempotencyKey());
        if (existingEntryOpt.isPresent()) {
            JournalEntry existing = existingEntryOpt.get();
            log.info("Idempotency match found for key={}. Returning existing transfer receipt id={}",
                    request.idempotencyKey(), existing.getId());
            validateMatchingTransferParameters(existing, request);
            return mapToTransferResponse(existing);
        }

        try {
            // Resolve sharded hot accounts to dilute contention
            UUID resolvedDestId = accountShardRouter.resolveDestinationAccount(request.destinationAccountId());

            // Deadlock-free pessimistic lock acquisition
            LockedAccountPair pair = acquireLocksDeterministically(request.sourceAccountId(), resolvedDestId);
            Account sourceAccount = pair.source();
            Account destAccount = pair.destination();

            validateCurrencies(sourceAccount, destAccount, request.currency());

            // Execute balance mutations
            sourceAccount.debit(request.amount());
            destAccount.credit(request.amount());

            accountRepository.save(sourceAccount);
            accountRepository.save(destAccount);

            // Construct immutable double-entry journal and posting lines
            JournalEntry journalEntry = JournalEntry.builder()
                    .idempotencyKey(request.idempotencyKey())
                    .currency(request.currency())
                    .description(request.description())
                    .build();

            PostingLine debitLine = PostingLine.builder()
                    .account(sourceAccount)
                    .amount(request.amount())
                    .direction(PostingType.DEBIT)
                    .build();

            PostingLine creditLine = PostingLine.builder()
                    .account(destAccount)
                    .amount(request.amount())
                    .direction(PostingType.CREDIT)
                    .build();

            journalEntry.addPostingLine(debitLine);
            journalEntry.addPostingLine(creditLine);

            JournalEntry savedEntry = journalEntryRepository.save(journalEntry);
            log.info("Successfully executed transfer: journalEntryId={}, from={}, to={}, amount={} {}",
                    savedEntry.getId(), sourceAccount.getId(), destAccount.getId(), request.amount(), request.currency());

            eventService.recordTransferCompleted(
                    savedEntry.getId(),
                    request.idempotencyKey(),
                    String.format("Transfer executed: %s %s from account %s to account %s",
                            request.amount(), request.currency(), sourceAccount.getId(), destAccount.getId())
            );

            return mapToTransferResponse(savedEntry);

        } catch (InsufficientBalanceException | CurrencyMismatchException | SameAccountTransferException | AccountNotFoundException ex) {
            log.warn("Transfer failed for idempotencyKey={}: {}", request.idempotencyKey(), ex.getMessage());
            eventService.recordTransferFailed(request.idempotencyKey(), ex.getMessage(), "Transfer execution aborted due to business rule validation");
            throw ex;
        }
    }

    /**
     * Fallback recovery invoked only when all retry attempts are exhausted due to DB lock contention or throttling.
     */
    @Recover
    public TransferResponse recover(Exception ex, TransferRequest request) {
        log.error("Exhausted all retry attempts for transfer idempotencyKey={}. Cause: {}",
                request.idempotencyKey(), ex.getMessage());
        eventService.recordTransferFailed(
                request.idempotencyKey(),
                "Database contention/throttling exhausted retries: " + ex.getMessage(),
                "TRANSFER_CONTENTION_EXHAUSTED"
        );
        throw new LedgerContentionException(request.idempotencyKey(), ex.getMessage(), ex);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransferById(UUID id) {
        log.debug("Fetching transfer receipt for id={}", id);
        JournalEntry entry = journalEntryRepository.findByIdWithLines(id)
                .orElseThrow(() -> {
                    log.warn("Transfer lookup failed: journal entry id={} not found", id);
                    return new IllegalArgumentException("Transfer not found with id: " + id);
                });

        return mapToTransferResponse(entry);
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("TransferRequest cannot be null");
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key is mandatory");
        }
        if (request.sourceAccountId() == null) {
            throw new IllegalArgumentException("Source account ID is mandatory");
        }
        if (request.destinationAccountId() == null) {
            throw new IllegalArgumentException("Destination account ID is mandatory");
        }
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new SameAccountTransferException(request.sourceAccountId());
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly positive");
        }
    }

    private void validateMatchingTransferParameters(JournalEntry existing, TransferRequest request) {
        if (existing.getCurrency() != request.currency()) {
            throw new DuplicateIdempotencyKeyException(request.idempotencyKey());
        }

        PostingLine debitLine = existing.getLines().stream()
                .filter(l -> l.getDirection() == PostingType.DEBIT)
                .findFirst()
                .orElse(null);

        PostingLine creditLine = existing.getLines().stream()
                .filter(l -> l.getDirection() == PostingType.CREDIT)
                .findFirst()
                .orElse(null);

        if (debitLine == null || creditLine == null
                || !debitLine.getAccountId().equals(request.sourceAccountId())
                || debitLine.getAmount().compareTo(request.amount()) != 0) {
            throw new DuplicateIdempotencyKeyException(request.idempotencyKey());
        }
    }

    private LockedAccountPair acquireLocksDeterministically(UUID sourceId, UUID destId) {
        boolean sourceFirst = sourceId.compareTo(destId) < 0;
        UUID firstLockId = sourceFirst ? sourceId : destId;
        UUID secondLockId = sourceFirst ? destId : sourceId;

        Account first = accountRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException(firstLockId));
        Account second = accountRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException(secondLockId));

        Account source = sourceFirst ? first : second;
        Account dest = sourceFirst ? second : first;

        return new LockedAccountPair(source, dest);
    }

    private void validateCurrencies(Account source, Account dest, Currency transferCurrency) {
        if (source.getCurrency() != dest.getCurrency()) {
            throw new CurrencyMismatchException(
                    source.getId(), source.getCurrency(),
                    dest.getId(), dest.getCurrency()
            );
        }

        if (source.getCurrency() != transferCurrency) {
            throw new IllegalArgumentException(String.format(
                    "Transfer currency %s does not match accounts currency %s",
                    transferCurrency, source.getCurrency()
            ));
        }
    }

    private TransferResponse mapToTransferResponse(JournalEntry entry) {
        List<PostingLineResponse> lineResponses = entry.getLines().stream()
                .map(line -> new PostingLineResponse(
                        line.getId(),
                        line.getAccountId(),
                        line.getAmount(),
                        line.getDirection()
                ))
                .toList();

        return new TransferResponse(
                entry.getId(),
                entry.getIdempotencyKey(),
                entry.getCurrency(),
                entry.getDescription(),
                entry.getCreatedAt(),
                lineResponses
        );
    }

    private record LockedAccountPair(Account source, Account destination) {}
}
