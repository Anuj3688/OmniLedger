package dev.fintech.omniledger.exception;

import java.util.UUID;

/**
 * Thrown when an Account cannot be located by its UUID.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID accountId) {
        super("Account not found with id: " + accountId);
    }
}
