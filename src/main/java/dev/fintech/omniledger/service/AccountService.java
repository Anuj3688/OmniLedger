package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.request.CreateAccountRequest;
import dev.fintech.omniledger.dto.response.AccountResponse;

import java.util.UUID;

/**
 * Service contract for financial account operations.
 */
public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountById(UUID id);
}
