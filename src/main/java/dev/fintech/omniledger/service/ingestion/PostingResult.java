package dev.fintech.omniledger.service.ingestion;

import dev.fintech.omniledger.model.JournalEntry;
import dev.fintech.omniledger.model.enums.RejectionCode;

public record PostingResult(
        boolean success,
        RejectionCode rejectionCode,
        String errorMessage,
        JournalEntry journalEntry
) {
    public static PostingResult success(JournalEntry entry) {
        return new PostingResult(true, null, null, entry);
    }

    public static PostingResult failure(RejectionCode code, String message) {
        return new PostingResult(false, code, message, null);
    }
}
