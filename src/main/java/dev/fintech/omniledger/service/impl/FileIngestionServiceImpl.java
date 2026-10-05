package dev.fintech.omniledger.service.impl;

import dev.fintech.omniledger.dto.response.FileIngestionJobResponse;
import dev.fintech.omniledger.dto.response.FileIngestionRejectionResponse;
import dev.fintech.omniledger.exception.DuplicateFileException;
import dev.fintech.omniledger.exception.FileIngestionJobNotFoundException;
import dev.fintech.omniledger.model.FileIngestionJob;
import dev.fintech.omniledger.model.enums.IngestionJobStatus;
import dev.fintech.omniledger.repository.FileIngestionJobRepository;
import dev.fintech.omniledger.repository.FileIngestionRejectionRepository;
import dev.fintech.omniledger.service.FileIngestionService;
import dev.fintech.omniledger.service.ingestion.AsyncFileIngestionProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestionServiceImpl implements FileIngestionService {

    private final FileIngestionJobRepository jobRepository;
    private final FileIngestionRejectionRepository rejectionRepository;
    private final AsyncFileIngestionProcessor asyncProcessor;

    @Override
    public FileIngestionJobResponse ingestPairedTransactionsFile(MultipartFile file, String sourceSystem) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be null or empty");
        }
        if (sourceSystem == null || sourceSystem.trim().isBlank()) {
            throw new IllegalArgumentException("sourceSystem is required");
        }

        try {
            return ingestPairedTransactionsStream(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv",
                    file.getInputStream(),
                    file.getSize(),
                    sourceSystem.trim().toUpperCase()
            );
        } catch (IOException ex) {
            log.error("Failed to read uploaded file stream: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to read uploaded file stream: " + ex.getMessage(), ex);
        }
    }

    @Override
    public FileIngestionJobResponse ingestPairedTransactionsStream(
            String fileName,
            InputStream inputStream,
            long fileSize,
            String sourceSystem
    ) {
        Path tempFile = null;
        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            tempFile = Files.createTempFile("omni_ingest_", ".tmp");

            try (DigestInputStream dis = new DigestInputStream(inputStream, sha256Digest)) {
                Files.copy(dis, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String fileChecksum = HexFormat.of().formatHex(sha256Digest.digest());
            log.info("Incoming file '{}' ({} bytes) computed SHA-256: {}", fileName, fileSize, fileChecksum);

            // Layer 1 Deduplication: Check if identical file was already uploaded
            Optional<FileIngestionJob> existingJobOpt = jobRepository.findByFileChecksumSha256(fileChecksum);
            if (existingJobOpt.isPresent()) {
                FileIngestionJob existing = existingJobOpt.get();
                if (existing.getStatus() == IngestionJobStatus.COMPLETED
                        || existing.getStatus() == IngestionJobStatus.PROCESSING
                        || existing.getStatus() == IngestionJobStatus.ACCEPTED
                        || existing.getStatus() == IngestionJobStatus.PENDING) {
                    log.warn("Duplicate file upload rejected. Existing jobId={}, checksum={}", existing.getJobId(), fileChecksum);
                    Files.deleteIfExists(tempFile);
                    throw new DuplicateFileException(
                            String.format("File has already been processed under Job ID: %s", existing.getJobId()),
                            existing.getJobId(),
                            fileChecksum
                    );
                }
            }

            FileIngestionJob job = FileIngestionJob.builder()
                    .fileName(fileName)
                    .fileChecksumSha256(fileChecksum)
                    .sourceSystem(sourceSystem)
                    .fileSizeBytes(fileSize)
                    .status(IngestionJobStatus.ACCEPTED)
                    .totalRecords(0)
                    .processedRecords(0)
                    .failedRecords(0)
                    .build();

            job = jobRepository.save(job);

            // Dispatch asynchronous processing on thread pool
            asyncProcessor.processFileAsync(job.getJobId(), tempFile, sourceSystem);

            // Fast return (<200ms) with HTTP 202 Accepted status
            return AsyncFileIngestionProcessor.mapToJobResponse(job);

        } catch (NoSuchAlgorithmException ex) {
            deleteTempFile(tempFile);
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        } catch (IOException ex) {
            deleteTempFile(tempFile);
            log.error("IO error while buffering file ingestion stream: {}", ex.getMessage(), ex);
            throw new IllegalStateException("IO error while buffering file ingestion stream: " + ex.getMessage(), ex);
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FileIngestionJobResponse getJobStatus(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(AsyncFileIngestionProcessor::mapToJobResponse)
                .orElseThrow(() -> new FileIngestionJobNotFoundException(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileIngestionRejectionResponse> getJobRejections(UUID jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new FileIngestionJobNotFoundException(jobId);
        }

        return rejectionRepository.findByJobJobIdOrderByRowNumberAsc(jobId).stream()
                .map(r -> FileIngestionRejectionResponse.builder()
                        .rejectionId(r.getRejectionId())
                        .jobId(r.getJob().getJobId())
                        .rowNumber(r.getRowNumber())
                        .rawRecord(r.getRawRecord())
                        .rejectionCode(r.getRejectionCode())
                        .reason(r.getReason())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
    }
}
