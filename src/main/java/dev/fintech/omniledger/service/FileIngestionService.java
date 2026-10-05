package dev.fintech.omniledger.service;

import dev.fintech.omniledger.dto.response.FileIngestionJobResponse;
import dev.fintech.omniledger.dto.response.FileIngestionRejectionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface FileIngestionService {

    FileIngestionJobResponse ingestPairedTransactionsFile(MultipartFile file, String sourceSystem);

    FileIngestionJobResponse ingestPairedTransactionsStream(String fileName, InputStream inputStream, long fileSize, String sourceSystem);

    FileIngestionJobResponse getJobStatus(UUID jobId);

    List<FileIngestionRejectionResponse> getJobRejections(UUID jobId);
}
