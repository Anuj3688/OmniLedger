package dev.fintech.omniledger.exception;

import dev.fintech.omniledger.model.enums.Currency;
import lombok.Getter;

import java.util.UUID;

/**
 * Thrown when attempting a transfer between accounts denominated in different currencies
 * without an intervening FX conversion leg.
 */
@Getter
public class CurrencyMismatchException extends RuntimeException {

    private final UUID sourceAccountId;
    private final Currency sourceCurrency;
    private final UUID destinationAccountId;
    private final Currency destinationCurrency;

    public CurrencyMismatchException(UUID sourceAccountId, Currency sourceCurrency,
                                     UUID destinationAccountId, Currency destinationCurrency) {
        super(String.format("Currency mismatch in transfer: source account %s is in %s, destination account %s is in %s",
                sourceAccountId, sourceCurrency, destinationAccountId, destinationCurrency));
        this.sourceAccountId = sourceAccountId;
        this.sourceCurrency = sourceCurrency;
        this.destinationAccountId = destinationAccountId;
        this.destinationCurrency = destinationCurrency;
    }
}
