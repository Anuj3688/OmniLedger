package dev.fintech.omniledger.dto;

import dev.fintech.omniledger.model.enums.AccountType;
import dev.fintech.omniledger.model.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Standard response payload representing an account's state.
 */
@Schema(description = "Account details and current ledger balance")
public record AccountResponse(
        @Schema(description = "Unique account identifier", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID accountId,

        @Schema(description = "Owner user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "Accounting classification", example = "LIABILITY")
        AccountType accountType,

        @Schema(description = "Current monetary balance", example = "10000.0000")
        BigDecimal balance,

        @Schema(description = "Account currency", example = "INR")
        Currency currency
) {}
