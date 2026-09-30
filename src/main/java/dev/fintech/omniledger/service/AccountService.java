package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.AccountResponse;
import dev.fintech.omniledger.dto.CreateAccountRequest;

import java.util.UUID;

/**
 * Service contract for account management operations.
 */
public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountById(UUID id);
}
