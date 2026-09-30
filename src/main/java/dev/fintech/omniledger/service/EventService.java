package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.SystemEventResponse;
import dev.fintech.omniledger.model.enums.EventType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * High-level audit and event management service facade.
 * Provides query methods and overloaded event recorders across different domain operations.
 */
public interface EventService {

    // --- Query APIs ---
    List<SystemEventResponse> getAllEvents();

    List<SystemEventResponse> getEventsBetween(Instant startTime, Instant endTime);

    // --- Domain Overloaded Event Recorders ---

    void recordAccountCreated(UUID accountId, UUID userId, String details);

    void recordTransferInitiated(UUID sourceAccountId, UUID destAccountId, String idempotencyKey, String details);

    void recordTransferCompleted(UUID journalEntryId, UUID sourceAccountId, UUID destAccountId, String idempotencyKey, String details);

    void recordTransferFailed(UUID sourceAccountId, UUID destAccountId, String idempotencyKey, String errorMessage);

    void recordGenericEvent(EventType type, UUID userId, UUID accountId, UUID journalEntryId, String idempotencyKey, String payload, String status, String errorMessage);
}
