package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.request.CreateAccountRequest;
import dev.fintech.omniledger.dto.response.AccountResponse;
import dev.fintech.omniledger.exception.AccountNotFoundException;
import dev.fintech.omniledger.exception.PartyNotFoundException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.Party;
import dev.fintech.omniledger.model.enums.Currency;
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
import java.util.UUID;

/**
 * Production implementation of AccountService managing account creation,
 * domain policy enforcement, balance checks, and lifecycle audits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PartyRepository partyRepository;
    private final AccountPolicyRegistry accountPolicyRegistry;
    private final EventService eventService;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Received request to create account: partyId={}, accountType={}, currency={}",
                request.partyId(), request.accountType(), request.currency());

        // Isolated validation and policy enforcement
        Party party = validateAccountCreation(request);

        Currency targetCurrency = request.currency() != null ? request.currency() : Currency.INR;
        BigDecimal initialBalance = request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO;

        Account account = Account.builder()
                .partyId(party.getId())
                .accountType(request.accountType())
                .currency(targetCurrency)
                .balance(initialBalance)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Successfully opened account id={} for partyId={} with type={} balance={} {}",
                savedAccount.getId(), savedAccount.getPartyId(), savedAccount.getAccountType(),
                savedAccount.getBalance(), savedAccount.getCurrency());

        // Emit audit event log
        eventService.recordAccountCreated(
                savedAccount.getId(),
                savedAccount.getPartyId(),
                String.format("Account opened: partyType=%s, type=%s, currency=%s, initialBalance=%s",
                        party.getPartyType(), savedAccount.getAccountType(), savedAccount.getCurrency(), savedAccount.getBalance())
        );

        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        log.debug("Fetching account details for id={}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Account lookup failed: account id={} not found", id);
                    return new AccountNotFoundException(id);
                });

        return mapToResponse(account);
    }

    /**
     * Dedicated validation method verifying structural constraints, party existence,
     * and polymorphic business policies via AccountPolicyRegistry.
     */
    private Party validateAccountCreation(CreateAccountRequest request) {
        log.debug("Validating account creation request parameters");

        if (request == null) {
            log.error("Account creation validation failed: request payload is null");
            throw new IllegalArgumentException("CreateAccountRequest cannot be null");
        }

        if (request.partyId() == null) {
            log.error("Account creation validation failed: partyId is missing");
            throw new IllegalArgumentException("PartyId is mandatory");
        }

        if (request.accountType() == null) {
            log.error("Account creation validation failed: accountType is missing");
            throw new IllegalArgumentException("AccountType is mandatory");
        }

        if (request.initialBalance() != null && request.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            log.error("Account creation validation failed: negative initial balance {}", request.initialBalance());
            throw new IllegalArgumentException("Initial balance cannot be negative: " + request.initialBalance());
        }

        Party party = partyRepository.findById(request.partyId())
                .orElseThrow(() -> {
                    log.warn("Account creation failed: Party id={} does not exist", request.partyId());
                    return new PartyNotFoundException(request.partyId());
                });

        // Enforce PartyType-specific account ownership policy (Strategy pattern, zero if-else)
        accountPolicyRegistry.validate(
                party.getPartyType(),
                party.getId(),
                request.accountType(),
                request.currency() != null ? request.currency() : Currency.INR
        );

        log.debug("Account creation request passed all validation and policy checks for partyId={}", party.getId());
        return party;
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getPartyId(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency()
        );
    }
}
