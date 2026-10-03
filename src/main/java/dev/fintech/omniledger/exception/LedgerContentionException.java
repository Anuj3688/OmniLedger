package dev.fintech.omniledger.exception;

import lombok.Getter;

/**
 * Thrown when high concurrency contention, lock wait timeouts, or database rate-limiting (429)
 * prevent transaction completion after exhausting all retry attempts.
 */
@Getter
public class LedgerContentionException extends RuntimeException {

    private final String idempotencyKey;

    public LedgerContentionException(String idempotencyKey, String message, Throwable cause) {
        super(String.format("Transfer execution failed due to database contention/throttling for key %s: %s",
                idempotencyKey, message), cause);
        this.idempotencyKey = idempotencyKey;
    }
}
