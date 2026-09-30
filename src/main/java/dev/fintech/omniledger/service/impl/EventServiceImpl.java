package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.SystemEventResponse;
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
 * Production implementation of EventService.
 * Uses REQUIRES_NEW propagation for event recording so audit logs persist even if an enclosing transaction rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final SystemEventRepository systemEventRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<SystemEventResponse> getAllEvents() {
        return systemEventRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemEventResponse> getEventsBetween(Instant startTime, Instant endTime) {
        return systemEventRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startTime, endTime)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAccountCreated(UUID accountId, UUID userId, String details) {
        eventPublisher.publish(EventRecordCommand.builder()
                .eventType(EventType.ACCOUNT_CREATED)
                .accountId(accountId)
                .userId(userId)
                .payload(details)
                .status("SUCCESS")
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransferInitiated(UUID sourceAccountId, UUID destAccountId, String idempotencyKey, String details) {
        eventPublisher.publish(EventRecordCommand.builder()
                .eventType(EventType.TRANSFER_INITIATED)
                .accountId(sourceAccountId)
                .idempotencyKey(idempotencyKey)
                .payload(details)
                .status("PENDING")
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransferCompleted(UUID journalEntryId, UUID sourceAccountId, UUID destAccountId, String idempotencyKey, String details) {
        eventPublisher.publish(EventRecordCommand.builder()
                .eventType(EventType.TRANSFER_COMPLETED)
                .journalEntryId(journalEntryId)
                .accountId(sourceAccountId)
                .idempotencyKey(idempotencyKey)
                .payload(details)
                .status("SUCCESS")
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransferFailed(UUID sourceAccountId, UUID destAccountId, String idempotencyKey, String errorMessage) {
        eventPublisher.publish(EventRecordCommand.builder()
                .eventType(EventType.TRANSFER_FAILED)
                .accountId(sourceAccountId)
                .idempotencyKey(idempotencyKey)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordGenericEvent(EventType type, UUID userId, UUID accountId, UUID journalEntryId,
                                   String idempotencyKey, String payload, String status, String errorMessage) {
        eventPublisher.publish(EventRecordCommand.builder()
                .eventType(type)
                .userId(userId)
                .accountId(accountId)
                .journalEntryId(journalEntryId)
                .idempotencyKey(idempotencyKey)
                .payload(payload)
                .status(status)
                .errorMessage(errorMessage)
                .build());
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
