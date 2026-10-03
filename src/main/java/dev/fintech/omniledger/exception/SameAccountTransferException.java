package dev.fintech.omniledger.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Thrown when a transfer specifies the same account as both source and destination.
 */
@Getter
public class SameAccountTransferException extends RuntimeException {

    private final UUID accountId;

    public SameAccountTransferException(UUID accountId) {
        super("Cannot execute transfer: source and destination account are identical (" + accountId + ")");
        this.accountId = accountId;
    }
}
