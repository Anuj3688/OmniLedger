package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.response.SystemEventResponse;
import dev.fintech.omniledger.model.SystemEvent;
import dev.fintech.omniledger.model.enums.EventType;
import dev.fintech.omniledger.repository.SystemEventRepository;
import dev.fintech.omniledger.service.EventPublisher;
import dev.fintech.omniledger.service.EventService;
import dev.fintech.omniledger.service.dto.EventRecordCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core event orchestrator and query service.
 * Write operations execute in REQUIRES_NEW transactions to isolate audit logging from caller rollbacks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventPublisher eventPublisher;
    private final SystemEventRepository systemEventRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAccountCreated(UUID accountId, UUID userId, String details) {
        log.info("Emitting ACCOUNT_CREATED event for accountId={}, userId={}", accountId, userId);
        EventRecordCommand command = new EventRecordCommand(
                EventType.ACCOUNT_CREATED,
                userId,
                accountId,
                null,
                null,
                details,
                "SUCCESS",
                null
        );
        eventPublisher.publish(command);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransferInitiated(UUID journalEntryId, String idempotencyKey, String details) {
        log.info("Emitting TRANSFER_INITIATED event for journalEntryId={}, idempotencyKey={}", journalEntryId, idempotencyKey);
        EventRecordCommand command = new EventRecordCommand(
                EventType.TRANSFER_INITIATED,
                null,
                null,
                journalEntryId,
                idempotencyKey,
                details,
                "PENDING",
                null
        );
        eventPublisher.publish(command);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransferCompleted(UUID journalEntryId, String idempotencyKey, String details) {
        log.info("Emitting TRANSFER_COMPLETED event for journalEntryId={}, idempotencyKey={}", journalEntryId, idempotencyKey);
        EventRecordCommand command = new EventRecordCommand(
                EventType.TRANSFER_COMPLETED,
                null,
                null,
                journalEntryId,
                idempotencyKey,
                details,
                "SUCCESS",
                null
        );
        eventPublisher.publish(command);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransferFailed(String idempotencyKey, String errorMessage, String details) {
        log.warn("Emitting TRANSFER_FAILED event for idempotencyKey={}, error={}", idempotencyKey, errorMessage);
        EventRecordCommand command = new EventRecordCommand(
                EventType.TRANSFER_FAILED,
                null,
                null,
                null,
                idempotencyKey,
                details,
                "FAILED",
                errorMessage
        );
        eventPublisher.publish(command);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordGenericEvent(EventType eventType, UUID userId, UUID accountId, UUID journalEntryId,
                                   String idempotencyKey, String payload, String status, String errorMessage) {
        log.debug("Emitting generic event: type={}, status={}, idempotencyKey={}", eventType, status, idempotencyKey);
        EventRecordCommand command = new EventRecordCommand(
                eventType,
                userId,
                accountId,
                journalEntryId,
                idempotencyKey,
                payload,
                status,
                errorMessage
        );
        eventPublisher.publish(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemEventResponse> getAllEvents() {
        log.debug("Fetching all system audit events ordered by createdAt DESC");
        return systemEventRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemEventResponse> getEventsByTimeRange(Instant startTime, Instant endTime) {
        log.debug("Fetching system events between {} and {}", startTime, endTime);
        return systemEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startTime, endTime)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private SystemEventResponse mapToResponse(SystemEvent event) {
        return new SystemEventResponse(
                event.getId(),
                event.getEventType(),
                event.getUserId(),
                event.getAccountId(),
                event.getJournalEntryId(),
                event.getIdempotencyKey(),
                event.getPayload(),
                event.getStatus(),
                event.getErrorMessage(),
                event.getCreatedAt()
        );
    }
}
