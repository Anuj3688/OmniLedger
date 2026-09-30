package dev.fintech.omniledger.model;

import dev.fintech.omniledger.model.enums.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable system and audit event tracking all operational actions across the ledger.
 */
@Entity
@Table(
        name = "system_events",
        indexes = {
                @Index(name = "idx_events_created_at", columnList = "created_at"),
                @Index(name = "idx_events_user_id", columnList = "user_id"),
                @Index(name = "idx_events_account_id", columnList = "account_id"),
                @Index(name = "idx_events_journal_entry_id", columnList = "journal_entry_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private EventType eventType;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "journal_entry_id", updatable = false)
    private UUID journalEntryId;

    @Column(name = "idempotency_key", length = 128, updatable = false)
    private String idempotencyKey;

    @Column(name = "payload", columnDefinition = "TEXT", updatable = false)
    private String payload;

    @Column(name = "status", nullable = false, length = 20, updatable = false)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT", updatable = false)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
