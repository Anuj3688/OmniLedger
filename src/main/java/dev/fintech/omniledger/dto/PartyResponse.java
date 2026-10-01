package dev.fintech.omniledger.dto;

import dev.fintech.omniledger.model.enums.PartyStatus;
import dev.fintech.omniledger.model.enums.PartyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Unified representation of a Party in the banking ecosystem.
 */
@Schema(description = "Representation of a registered Party in OmniLedger")
public record PartyResponse(
        @Schema(description = "Unique Party identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID partyId,

        @Schema(description = "Party classification", example = "INDIVIDUAL")
        PartyType partyType,

        @Schema(description = "Current operational status", example = "ACTIVE")
        PartyStatus status,

        @Schema(description = "Primary display name (Full name, Business name, or Department)", example = "Rahul Sharma")
        String displayName,

        @Schema(description = "Tax or official statutory identifier (PAN, GSTIN, or Nodal code)", example = "ABCDE1234F")
        String statutoryIdentifier,

        @Schema(description = "Creation timestamp", example = "2026-10-01T22:00:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp", example = "2026-10-01T22:00:00Z")
        Instant updatedAt
) {}
