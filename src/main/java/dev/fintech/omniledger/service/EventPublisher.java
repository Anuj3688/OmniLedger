package dev.fintech.omniledger.service;

import dev.fintech.omniledger.service.dto.EventRecordCommand;

/**
 * Output port / strategy interface for publishing and recording system events.
 * Implementations can persist locally (database), send to Kafka, RabbitMQ, or an external audit microservice.
 */
public interface EventPublisher {

    void publish(EventRecordCommand command);
}
