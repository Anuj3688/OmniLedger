package dev.fintech.omniledger.exception;

import lombok.Getter;

/**
 * Thrown when an idempotency key conflict occurs with conflicting payload parameters.
 */
@Getter
public class DuplicateIdempotencyKeyException extends RuntimeException {

    private final String idempotencyKey;

    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super("A transfer with idempotency key '" + idempotencyKey + "' has already been executed with different parameters.");
        this.idempotencyKey = idempotencyKey;
    }
}
