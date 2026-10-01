package dev.fintech.omniledger.dto;

import dev.fintech.omniledger.model.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload for executing an ACID-compliant, double-entry financial transfer.
 */
@Schema(description = "Request payload for executing a financial transfer between accounts")
public record TransferRequest(
        @Schema(description = "Unique client-generated idempotency key to prevent duplicate transfers", example = "tx-2026-10-01-001", requiredMode = Schema.RequiredMode.REQUIRED)
        String idempotencyKey,

        @Schema(description = "UUID of the account to be debited", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID sourceAccountId,

        @Schema(description = "UUID of the account to be credited", example = "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID destinationAccountId,

        @Schema(description = "Strictly positive monetary amount to transfer", example = "500.0000", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Transaction currency", example = "INR", defaultValue = "INR")
        Currency currency,

        @Schema(description = "Narrative context or memo for the transfer", example = "P2P transfer from Alice to Bob")
        String description
) {
    public TransferRequest {
        if (currency == null) {
            currency = Currency.INR;
        }
    }
}
