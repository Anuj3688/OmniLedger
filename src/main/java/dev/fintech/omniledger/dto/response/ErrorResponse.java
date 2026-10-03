package dev.fintech.omniledger.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standardized API error response payload.
 */
@Schema(description = "Standardized error response")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "Error categorization", example = "NOT_FOUND")
        String error,

        @Schema(description = "Human-readable root cause message", example = "Account not found with id: ...")
        String message,

        @Schema(description = "Timestamp of the error", example = "2026-10-03T19:00:00Z")
        Instant timestamp
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, Instant.now());
    }
}
