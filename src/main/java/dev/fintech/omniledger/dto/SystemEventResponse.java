package dev.fintech.omniledger.dto;

import dev.fintech.omniledger.model.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed representation of an immutable system audit event.
 */
@Schema(description = "System audit event log record")
public record SystemEventResponse(
        @Schema(description = "Unique event identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID eventId,

        @Schema(description = "Event category", example = "ACCOUNT_CREATED")
        EventType eventType,

        @Schema(description = "Associated user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "Associated account identifier", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID accountId,

        @Schema(description = "Associated journal entry identifier", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID journalEntryId,

        @Schema(description = "Idempotency key for correlation", example = "tx-1001-xyz")
        String idempotencyKey,

        @Schema(description = "Metadata or snapshot payload", example = "Account opened with balance 0.0000")
        String payload,

        @Schema(description = "Execution status", example = "SUCCESS")
        String status,

        @Schema(description = "Error details if failed", example = "null")
        String errorMessage,

        @Schema(description = "Timestamp when the event occurred", example = "2026-10-01T00:58:00Z")
        Instant createdAt
) {}
