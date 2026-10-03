package dev.fintech.omniledger.dto.request;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull(message = "Party ID is mandatory")
        UUID partyId,

        @NotNull(message = "Account type is mandatory")
        AccountType accountType,

        @NotNull(message = "Currency is mandatory")
        Currency currency,

        @NotNull(message = "Initial balance is mandatory")
        @DecimalMin(value = "0.0000", message = "Initial balance cannot be negative")
        BigDecimal initialBalance,

        Boolean isSharded,

        Integer shardCount
) {
    public CreateAccountRequest(UUID partyId, AccountType accountType, Currency currency, BigDecimal initialBalance) {
        this(partyId, accountType, currency, initialBalance, false, null);
    }
}
