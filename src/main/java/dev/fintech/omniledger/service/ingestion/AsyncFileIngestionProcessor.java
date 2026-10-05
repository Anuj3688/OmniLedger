package dev.fintech.omniledger.service.ingestion;

import dev.fintech.omniledger.dto.response.FileIngestionJobResponse;
import dev.fintech.omniledger.model.FileIngestionJob;
import dev.fintech.omniledger.model.FileIngestionRejection;
import dev.fintech.omniledger.model.enums.Currency;
import dev.fintech.omniledger.model.enums.EventType;
import dev.fintech.omniledger.model.enums.IngestionJobStatus;
import dev.fintech.omniledger.model.enums.PostingType;
import dev.fintech.omniledger.model.enums.RejectionCode;
import dev.fintech.omniledger.repository.FileIngestionJobRepository;
import dev.fintech.omniledger.repository.FileIngestionRejectionRepository;
import dev.fintech.omniledger.service.EventService;
import dev.fintech.omniledger.service.ExternalAccountMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncFileIngestionProcessor {

    private final FileIngestionJobRepository jobRepository;
    private final FileIngestionRejectionRepository rejectionRepository;
    private final ExternalAccountMappingService externalAccountMappingService;
    private final FileTransactionPostingProcessor postingProcessor;
    private final EventService eventService;

    @Async("fileIngestionExecutor")
    public CompletableFuture<FileIngestionJobResponse> processFileAsync(UUID jobId, Path tempFile, String sourceSystem) {
        log.info("Worker thread [{}] started async processing for jobId={}", Thread.currentThread().getName(), jobId);
        FileIngestionJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("Async processor could not find job with ID: {}", jobId);
            deleteTempFile(tempFile);
            return CompletableFuture.completedFuture(null);
        }

        try {
            job.setStatus(IngestionJobStatus.PROCESSING);
            job = jobRepository.save(job);

            int processedCount = 0;
            int failedCount = 0;

            Map<String, List<ParsedPostingLine>> transactionGroups = new LinkedHashMap<>();
            Map<String, UUID> resolvedAccounts = new LinkedHashMap<>();

            try (BufferedReader reader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8)) {
                String line;
                int rowNumber = 0;

                while ((line = reader.readLine()) != null) {
                    rowNumber++;
                    String trimmed = line.trim();

                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }

                    if (rowNumber == 1 && isHeaderLine(trimmed)) {
                        continue;
                    }

                    ParsedLineResult parseResult = parseCsvLine(trimmed, rowNumber);

                    if (!parseResult.isValid()) {
                        failedCount++;
                        saveRejection(job, rowNumber, trimmed, parseResult.rejectionCode(), parseResult.errorMessage());
                        continue;
                    }

                    ParsedPostingLine parsedLine = parseResult.parsedLine();
                    transactionGroups.computeIfAbsent(parsedLine.referenceId(), k -> new ArrayList<>()).add(parsedLine);

                    if (!resolvedAccounts.containsKey(parsedLine.externalAccountId())) {
                        Optional<UUID> resolvedOpt = externalAccountMappingService.resolveAccountId(sourceSystem, parsedLine.externalAccountId());
                        resolvedOpt.ifPresent(uuid -> resolvedAccounts.put(parsedLine.externalAccountId(), uuid));
                    }
                }
            }

            for (Map.Entry<String, List<ParsedPostingLine>> entry : transactionGroups.entrySet()) {
                String refId = entry.getKey();
                List<ParsedPostingLine> groupLines = entry.getValue();

                PostingResult result = postingProcessor.postTransactionGroup(sourceSystem, refId, groupLines, resolvedAccounts);

                if (result.success()) {
                    processedCount += groupLines.size();
                } else {
                    failedCount += groupLines.size();
                    for (ParsedPostingLine l : groupLines) {
                        saveRejection(job, l.rowNumber(), l.rawRecord(), result.rejectionCode(), result.errorMessage());
                    }
                }
            }

            job.setTotalRecords(processedCount + failedCount);
            job.setProcessedRecords(processedCount);
            job.setFailedRecords(failedCount);

            if (failedCount == 0 && processedCount > 0) {
                job.setStatus(IngestionJobStatus.COMPLETED);
            } else if (processedCount == 0 && failedCount > 0) {
                job.setStatus(IngestionJobStatus.FAILED);
                job.setErrorMessage("All records in the file failed validation.");
            } else if (failedCount > 0) {
                job.setStatus(IngestionJobStatus.PARTIALLY_FAILED);
                job.setErrorMessage(failedCount + " records quarantined due to validation errors.");
            } else {
                job.setStatus(IngestionJobStatus.COMPLETED);
            }

            job = jobRepository.save(job);

            EventType eventType = job.getStatus() == IngestionJobStatus.FAILED
                    ? EventType.FILE_INGESTION_FAILED
                    : EventType.FILE_INGESTION_COMPLETED;

            eventService.recordGenericEvent(
                    eventType,
                    null,
                    null,
                    null,
                    job.getFileChecksumSha256(),
                    String.format("File ingestion job %s: status=%s, total=%d, processed=%d, failed=%d",
                            job.getJobId(), job.getStatus(), job.getTotalRecords(), job.getProcessedRecords(), job.getFailedRecords()),
                    job.getStatus().name(),
                    job.getErrorMessage()
            );

            log.info("Async processing finished for jobId={}: status={}, processed={}, failed={}",
                    job.getJobId(), job.getStatus(), processedCount, failedCount);

            return CompletableFuture.completedFuture(mapToJobResponse(job));

        } catch (Exception ex) {
            log.error("Unexpected error during async file processing for jobId={}: {}", jobId, ex.getMessage(), ex);
            job.setStatus(IngestionJobStatus.FAILED);
            job.setErrorMessage("Internal processing error: " + ex.getMessage());
            jobRepository.save(job);
            return CompletableFuture.completedFuture(mapToJobResponse(job));
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    private boolean isHeaderLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("reference") || lower.contains("account") || lower.contains("amount") || lower.contains("direction");
    }

    private ParsedLineResult parseCsvLine(String line, int rowNumber) {
        String[] tokens = line.split(",", -1);
        if (tokens.length < 5) {
            return ParsedLineResult.failure(RejectionCode.MALFORMED_RECORD, "Line contains fewer than 5 columns: " + tokens.length);
        }

        String referenceId = cleanToken(tokens[0]);
        String externalAccountId = cleanToken(tokens[1]);
        String amountStr = cleanToken(tokens[2]);
        String directionStr = cleanToken(tokens[3]).toUpperCase();
        String currencyStr = cleanToken(tokens[4]).toUpperCase();
        String description = tokens.length > 5 ? cleanToken(tokens[5]) : null;

        if (referenceId.isEmpty()) {
            return ParsedLineResult.failure(RejectionCode.MALFORMED_RECORD, "reference_id cannot be blank");
        }
        if (externalAccountId.isEmpty()) {
            return ParsedLineResult.failure(RejectionCode.MALFORMED_RECORD, "external_account_id cannot be blank");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ParsedLineResult.failure(RejectionCode.INVALID_AMOUNT, "Amount must be strictly positive: " + amountStr);
            }
            if (amount.scale() > 4) {
                return ParsedLineResult.failure(RejectionCode.INVALID_AMOUNT, "Amount exceeds 4 decimal places precision: " + amountStr);
            }
        } catch (NumberFormatException ex) {
            return ParsedLineResult.failure(RejectionCode.INVALID_AMOUNT, "Invalid monetary number format: " + amountStr);
        }

        PostingType direction;
        try {
            direction = PostingType.valueOf(directionStr);
        } catch (IllegalArgumentException ex) {
            return ParsedLineResult.failure(RejectionCode.MALFORMED_RECORD, "Invalid posting direction: " + directionStr);
        }

        Currency currency;
        try {
            currency = Currency.valueOf(currencyStr);
        } catch (IllegalArgumentException ex) {
            return ParsedLineResult.failure(RejectionCode.INVALID_CURRENCY, "Unsupported currency code: " + currencyStr);
        }

        return ParsedLineResult.success(new ParsedPostingLine(
                rowNumber,
                line,
                referenceId,
                externalAccountId,
                amount,
                direction,
                currency,
                description
        ));
    }

    private String cleanToken(String token) {
        String trimmed = token.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private void saveRejection(FileIngestionJob job, int rowNumber, String rawRecord, RejectionCode code, String reason) {
        FileIngestionRejection rejection = FileIngestionRejection.builder()
                .job(job)
                .rowNumber(rowNumber)
                .rawRecord(rawRecord)
                .rejectionCode(code != null ? code : RejectionCode.MALFORMED_RECORD)
                .reason(reason)
                .build();
        rejectionRepository.save(rejection);
    }

    public static FileIngestionJobResponse mapToJobResponse(FileIngestionJob job) {
        return FileIngestionJobResponse.builder()
                .jobId(job.getJobId())
                .fileName(job.getFileName())
                .fileChecksumSha256(job.getFileChecksumSha256())
                .sourceSystem(job.getSourceSystem())
                .fileSizeBytes(job.getFileSizeBytes())
                .totalRecords(job.getTotalRecords())
                .processedRecords(job.getProcessedRecords())
                .failedRecords(job.getFailedRecords())
                .status(job.getStatus())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private record ParsedLineResult(
            boolean isValid,
            ParsedPostingLine parsedLine,
            RejectionCode rejectionCode,
            String errorMessage
    ) {
        public static ParsedLineResult success(ParsedPostingLine line) {
            return new ParsedLineResult(true, line, null, null);
        }

        public static ParsedLineResult failure(RejectionCode code, String message) {
            return new ParsedLineResult(false, null, code, message);
        }
    }
}
