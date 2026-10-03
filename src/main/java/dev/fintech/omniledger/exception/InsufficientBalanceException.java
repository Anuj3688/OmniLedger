package dev.fintech.omniledger.exception;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Thrown when an account does not have sufficient cleared balance to fulfill a debit operation.
 */
@Getter
public class InsufficientBalanceException extends RuntimeException {

    private final UUID accountId;
    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(UUID accountId, BigDecimal availableBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient balance in account %s: available=%s, requested=%s",
                accountId, availableBalance, requestedAmount));
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }
}
