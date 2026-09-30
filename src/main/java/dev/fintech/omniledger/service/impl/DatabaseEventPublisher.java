package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.model.SystemEvent;
import dev.fintech.omniledger.repository.SystemEventRepository;
import dev.fintech.omniledger.service.EventPublisher;
import dev.fintech.omniledger.service.dto.EventRecordCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Database-backed event publisher persisting audit records via JPA.
 */
@Slf4j
@RequiredArgsConstructor
public class DatabaseEventPublisher implements EventPublisher {

    private final SystemEventRepository systemEventRepository;

    @Override
    public void publish(EventRecordCommand command) {
        SystemEvent event = SystemEvent.builder()
                .eventType(command.eventType())
                .userId(command.userId())
                .accountId(command.accountId())
                .journalEntryId(command.journalEntryId())
                .idempotencyKey(command.idempotencyKey())
                .payload(command.payload())
                .status(command.status())
                .errorMessage(command.errorMessage())
                .build();

        systemEventRepository.save(event);
        log.debug("Recorded system event: type={}, status={}, id={}", command.eventType(), command.status(), event.getId());
    }
}
