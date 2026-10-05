package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.FileIngestionRejection;
import dev.fintech.omniledger.model.enums.RejectionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileIngestionRejectionRepository extends JpaRepository<FileIngestionRejection, UUID> {

    List<FileIngestionRejection> findByJobJobIdOrderByRowNumberAsc(UUID jobId);

    List<FileIngestionRejection> findByJobJobIdAndRejectionCode(UUID jobId, RejectionCode rejectionCode);

    long countByJobJobId(UUID jobId);
}
