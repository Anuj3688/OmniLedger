package dev.fintech.omniledger.dto.response;

import dev.fintech.omniledger.model.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Audit receipt representing a committed double-entry transfer and its balanced posting legs.
 */
@Schema(description = "Audit receipt representing a committed double-entry transfer")
public record TransferResponse(
        @Schema(description = "Unique journal entry header identifier", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID journalEntryId,

        @Schema(description = "Idempotency key associated with this transfer", example = "tx-2026-10-01-001")
        String idempotencyKey,

        @Schema(description = "Transaction currency", example = "INR")
        Currency currency,

        @Schema(description = "Transfer narrative or memo", example = "P2P transfer from Alice to Bob")
        String description,

        @Schema(description = "Timestamp when the transaction was committed", example = "2026-10-01T21:45:00Z")
        Instant createdAt,

        @Schema(description = "Balanced debit and credit legs of the transaction")
        List<PostingLineResponse> lines
) {}
