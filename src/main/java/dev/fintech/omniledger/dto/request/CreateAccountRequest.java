package dev.fintech.omniledger.dto.request;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload for opening a new financial account owned by a legal Party.
 */
@Schema(description = "Payload for opening a new financial account")
public record CreateAccountRequest(
        @Schema(description = "UUID of the legal Party owning this account", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID partyId,

        @Schema(description = "Classification of the account", example = "ASSET", requiredMode = Schema.RequiredMode.REQUIRED)
        AccountType accountType,

        @Schema(description = "Account currency", example = "INR", defaultValue = "INR")
        Currency currency,

        @Schema(description = "Initial balance for the account", example = "0.0000", defaultValue = "0.0000")
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
