package dev.fintech.omniledger.controller;

import dev.fintech.omniledger.dto.response.FileIngestionJobResponse;
import dev.fintech.omniledger.dto.response.FileIngestionRejectionResponse;
import dev.fintech.omniledger.service.FileIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "File Ingestion", description = "Endpoints for streaming third-party ledger files with SHA-256 deduplication and batch posting")
@RestController
@RequestMapping("/api/v1/ledger/files")
@RequiredArgsConstructor
public class FileIngestionController {

    private final FileIngestionService fileIngestionService;

    @Operation(
            summary = "Ingest paired transactions file asynchronously",
            description = "Accepts and streams a CSV containing paired posting lines, validates SHA-256 deduplication synchronously, and processes batches asynchronously to prevent API gateway timeouts."
    )
    @ApiResponse(responseCode = "202", description = "File accepted for background batch processing")
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileIngestionJobResponse> ingestPairedFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceSystem") String sourceSystem
    ) {
        FileIngestionJobResponse response = fileIngestionService.ingestPairedTransactionsFile(file, sourceSystem);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Get ingestion job status", description = "Retrieves the progress, record counts, and status of a file ingestion job")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<FileIngestionJobResponse> getJobStatus(@PathVariable("jobId") UUID jobId) {
        FileIngestionJobResponse response = fileIngestionService.getJobStatus(jobId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get ingestion job rejections", description = "Retrieves quarantined rejection records for audit and reconciliation")
    @GetMapping("/jobs/{jobId}/rejections")
    public ResponseEntity<List<FileIngestionRejectionResponse>> getJobRejections(@PathVariable("jobId") UUID jobId) {
        List<FileIngestionRejectionResponse> rejections = fileIngestionService.getJobRejections(jobId);
        return ResponseEntity.ok(rejections);
    }
}
