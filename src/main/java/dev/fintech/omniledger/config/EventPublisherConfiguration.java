package dev.fintech.omniledger.config;

import dev.fintech.omniledger.repository.SystemEventRepository;
import dev.fintech.omniledger.service.EventPublisher;
import dev.fintech.omniledger.service.impl.DatabaseEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enterprise configuration for the event publishing subsystem.
 * Employs both @ConditionalOnMissingBean (for programmatic overrides)
 * and @ConditionalOnProperty (for configuration-driven switches).
 */
@Configuration
public class EventPublisherConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    @ConditionalOnProperty(name = "omniledger.events.publisher.type", havingValue = "database", matchIfMissing = true)
    public EventPublisher databaseEventPublisher(SystemEventRepository systemEventRepository) {
        return new DatabaseEventPublisher(systemEventRepository);
    }
}
