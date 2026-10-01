package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.response.SystemEventResponse;
import dev.fintech.omniledger.model.enums.EventType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service contract for publishing and querying audit and operational events.
 */
public interface EventService {

    void recordAccountCreated(UUID accountId, UUID userId, String details);

    void recordTransferInitiated(UUID journalEntryId, String idempotencyKey, String details);

    void recordTransferCompleted(UUID journalEntryId, String idempotencyKey, String details);

    void recordTransferFailed(String idempotencyKey, String errorMessage, String details);

    void recordGenericEvent(EventType eventType, UUID userId, UUID accountId, UUID journalEntryId,
                            String idempotencyKey, String payload, String status, String errorMessage);

    List<SystemEventResponse> getAllEvents();

    List<SystemEventResponse> getEventsByTimeRange(Instant startTime, Instant endTime);
}
