package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.request.CreateAccountRequest;
import dev.fintech.omniledger.dto.response.AccountResponse;
import dev.fintech.omniledger.exception.AccountNotFoundException;
import dev.fintech.omniledger.exception.PartyNotFoundException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.Party;
import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.EventType;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.PartyRepository;
import dev.fintech.omniledger.service.AccountService;
import dev.fintech.omniledger.service.EventService;
import dev.fintech.omniledger.service.policy.AccountPolicyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Production implementation of AccountService orchestrating policy validation,
 * statutory party rules, and account provisioning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PartyRepository partyRepository;
    private final AccountPolicyRegistry accountPolicyRegistry;
    private final EventService eventService;

    private static final int DEFAULT_SHARD_COUNT = 10;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Received request to create account: partyId={}, accountType={}, currency={}",
                request.partyId(), request.accountType(), request.currency());

        validateAccountCreationRequest(request);

        Party party = partyRepository.findById(request.partyId())
                .orElseThrow(() -> {
                    log.warn("Account creation failed: Party id={} does not exist", request.partyId());
                    return new PartyNotFoundException(request.partyId());
                });

        accountPolicyRegistry.validate(party.getPartyType(), party.getId(), request.accountType(), request.currency());

        boolean isSharded = Boolean.TRUE.equals(request.isSharded());
        int shardCount = (request.shardCount() != null && request.shardCount() > 0)
                ? request.shardCount()
                : DEFAULT_SHARD_COUNT;

        Account masterAccount = Account.builder()
                .partyId(party.getId())
                .accountType(request.accountType())
                .currency(request.currency())
                .balance(isSharded ? BigDecimal.ZERO : request.initialBalance())
                .isSharded(isSharded)
                .parentAccountId(null)
                .build();

        Account savedMaster = accountRepository.save(masterAccount);

        if (isSharded) {
            log.info("Provisioning {} child shards for master accountId={}", shardCount, savedMaster.getId());
            List<Account> shards = new ArrayList<>(shardCount);
            for (int i = 0; i < shardCount; i++) {
                Account shard = Account.builder()
                        .partyId(party.getId())
                        .accountType(request.accountType())
                        .currency(request.currency())
                        .balance(i == 0 ? request.initialBalance() : BigDecimal.ZERO)
                        .isSharded(false)
                        .parentAccountId(savedMaster.getId())
                        .build();
                shards.add(shard);
            }
            accountRepository.saveAll(shards);
        }

        log.info("Successfully opened account id={} for partyId={} with type={} balance={} {} (sharded={})",
                savedMaster.getId(), party.getId(), savedMaster.getAccountType(),
                request.initialBalance(), savedMaster.getCurrency(), isSharded);

        eventService.recordAccountCreated(
                savedMaster.getId(),
                party.getId(),
                String.format("Account created: type=%s, currency=%s, initialBalance=%s, isSharded=%s",
                        savedMaster.getAccountType(), savedMaster.getCurrency(), request.initialBalance(), isSharded)
        );

        return mapToAccountResponse(savedMaster, request.initialBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        log.debug("Fetching account details for accountId={}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("Account lookup failed: accountId={} not found", accountId);
                    return new AccountNotFoundException(accountId);
                });

        BigDecimal effectiveBalance = account.isSharded()
                ? accountRepository.getAggregateBalance(accountId)
                : account.getBalance();

        return mapToAccountResponse(account, effectiveBalance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByPartyId(UUID partyId) {
        log.debug("Fetching all accounts for partyId={}", partyId);
        return accountRepository.findByPartyId(partyId)
                .stream()
                .filter(acc -> acc.getParentAccountId() == null) // Show master accounts and regular accounts
                .map(acc -> {
                    BigDecimal balance = acc.isSharded()
                            ? accountRepository.getAggregateBalance(acc.getId())
                            : acc.getBalance();
                    return mapToAccountResponse(acc, balance);
                })
                .toList();
    }

    private void validateAccountCreationRequest(CreateAccountRequest request) {
        log.debug("Validating account creation request parameters");
        if (request == null) {
            throw new IllegalArgumentException("CreateAccountRequest cannot be null");
        }
        if (request.partyId() == null) {
            throw new IllegalArgumentException("Party ID is mandatory");
        }
        if (request.accountType() == null) {
            throw new IllegalArgumentException("Account type is mandatory");
        }
        if (request.currency() == null) {
            throw new IllegalArgumentException("Currency is mandatory");
        }
        if (request.initialBalance() == null) {
            throw new IllegalArgumentException("Initial balance is mandatory");
        }
        if (request.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            log.error("Account creation validation failed: negative initial balance {}", request.initialBalance());
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
    }

    private AccountResponse mapToAccountResponse(Account account, BigDecimal effectiveBalance) {
        return new AccountResponse(
                account.getId(),
                account.getPartyId(),
                account.getAccountType(),
                account.getCurrency(),
                effectiveBalance,
                account.isSharded(),
                account.getParentAccountId()
        );
    }
}
