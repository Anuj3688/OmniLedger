package dev.fintech.omniledger.exception;

import java.util.UUID;

public class FileIngestionJobNotFoundException extends RuntimeException {
    public FileIngestionJobNotFoundException(UUID jobId) {
        super("File ingestion job not found with ID: " + jobId);
    }
}
