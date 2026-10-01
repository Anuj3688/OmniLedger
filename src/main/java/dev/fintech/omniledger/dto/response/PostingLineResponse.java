package dev.fintech.omniledger.dto.response;

import dev.fintech.omniledger.model.enums.PostingType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representation of an individual debit or credit leg within a transfer receipt.
 */
@Schema(description = "Individual debit or credit posting leg")
public record PostingLineResponse(
        @Schema(description = "Unique posting leg identifier", example = "c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f")
        UUID postingId,

        @Schema(description = "Associated account identifier", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID accountId,

        @Schema(description = "Transacted monetary amount", example = "500.0000")
        BigDecimal amount,

        @Schema(description = "Direction of money movement", example = "DEBIT")
        PostingType direction
) {}
