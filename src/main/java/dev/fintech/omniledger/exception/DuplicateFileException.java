package dev.fintech.omniledger.exception;

import java.util.UUID;

public class DuplicateFileException extends RuntimeException {

    private final UUID existingJobId;
    private final String checksum;

    public DuplicateFileException(String message, UUID existingJobId, String checksum) {
        super(message);
        this.existingJobId = existingJobId;
        this.checksum = checksum;
    }

    public UUID getExistingJobId() {
        return existingJobId;
    }

    public String getChecksum() {
        return checksum;
    }
}
