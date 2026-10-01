package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.AccountResponse;
import dev.fintech.omniledger.dto.CreateAccountRequest;
import dev.fintech.omniledger.exception.AccountNotFoundException;
import dev.fintech.omniledger.exception.UserNotFoundException;
import dev.fintech.omniledger.model.Account;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.repository.AccountRepository;
import dev.fintech.omniledger.repository.UserRepository;
import dev.fintech.omniledger.service.AccountService;
import dev.fintech.omniledger.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Production implementation of AccountService managing account creation, balance checks, and lifecycle audits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Received request to create account: userId={}, accountType={}, currency={}",
                request.userId(), request.accountType(), request.currency());

        // Isolated validation step
        validateAccountCreation(request);

        Currency targetCurrency = request.currency() != null ? request.currency() : Currency.INR;
        BigDecimal initialBalance = request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO;

        Account account = Account.builder()
                .userId(request.userId())
                .accountType(request.accountType())
                .currency(targetCurrency)
                .balance(initialBalance)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Successfully opened account id={} for userId={} with type={} balance={} {}",
                savedAccount.getId(), savedAccount.getUserId(), savedAccount.getAccountType(),
                savedAccount.getBalance(), savedAccount.getCurrency());

        // Emit audit event log
        eventService.recordAccountCreated(
                savedAccount.getId(),
                savedAccount.getUserId(),
                String.format("Account opened: type=%s, currency=%s, initialBalance=%s",
                        savedAccount.getAccountType(), savedAccount.getCurrency(), savedAccount.getBalance())
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
     * Dedicated validation method isolating all business constraints for account creation.
     */
    private void validateAccountCreation(CreateAccountRequest request) {
        log.debug("Validating account creation request parameters");

        if (request == null) {
            log.error("Account creation validation failed: request payload is null");
            throw new IllegalArgumentException("CreateAccountRequest cannot be null");
        }

        if (request.accountType() == null) {
            log.error("Account creation validation failed: accountType is missing");
            throw new IllegalArgumentException("AccountType is mandatory");
        }

        if (request.initialBalance() != null && request.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            log.error("Account creation validation failed: negative initial balance {}", request.initialBalance());
            throw new IllegalArgumentException("Initial balance cannot be negative: " + request.initialBalance());
        }

        if (request.userId() != null) {
            log.debug("Verifying owning user existence for userId={}", request.userId());
            if (!userRepository.existsById(request.userId())) {
                log.warn("Account creation validation failed: user id={} does not exist", request.userId());
                throw new UserNotFoundException(request.userId());
            }
        }

        log.debug("Account creation request passed all validation checks");
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency()
        );
    }
}
