package dev.fintech.omniledger.repository;

import dev.fintech.omniledger.model.FileIngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileIngestionJobRepository extends JpaRepository<FileIngestionJob, UUID> {

    Optional<FileIngestionJob> findByFileChecksumSha256(String fileChecksumSha256);

    boolean existsByFileChecksumSha256(String fileChecksumSha256);
}
