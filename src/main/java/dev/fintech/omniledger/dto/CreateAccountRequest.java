package dev.fintech.omniledger.dto;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload for opening a new financial account.
 */
@Schema(description = "Request payload for creating a new financial account")
public record CreateAccountRequest(
        @Schema(description = "Owner user UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "Accounting classification", example = "ASSET", requiredMode = Schema.RequiredMode.REQUIRED)
        AccountType accountType,

        @Schema(description = "Currency for the account", example = "INR", defaultValue = "INR")
        Currency currency,

        @Schema(description = "Initial opening balance", example = "0.0000", defaultValue = "0.0000")
        BigDecimal initialBalance
) {
    public CreateAccountRequest {
        if (currency == null) {
            currency = Currency.INR;
        }
        if (initialBalance == null) {
            initialBalance = BigDecimal.ZERO;
        }
    }
}
