package dev.fintech.omniledger.model;

import dev.fintech.omniledger.model.enums.RejectionCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_ingestion_rejections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileIngestionRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rejection_id", updatable = false, nullable = false)
    private UUID rejectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false, updatable = false)
    private FileIngestionJob job;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "raw_record", nullable = false, columnDefinition = "TEXT")
    private String rawRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_code", nullable = false, length = 64)
    private RejectionCode rejectionCode;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
