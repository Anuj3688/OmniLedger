package dev.fintech.omniledger.service.dto;

import dev.fintech.omniledger.model.enums.EventType;
import lombok.Builder;

import java.util.UUID;

/**
 * Immutable command object representing an event to be published and recorded.
 * Decoupled from any specific storage technology.
 */
@Builder
public record EventRecordCommand(
        EventType eventType,
        UUID userId,
        UUID accountId,
        UUID journalEntryId,
        String idempotencyKey,
        String payload,
        String status,
        String errorMessage
) {}
