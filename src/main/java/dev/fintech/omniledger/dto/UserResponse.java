package dev.fintech.omniledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard response payload representing a registered user profile.
 */
@Schema(description = "Customer profile and identification details")
public record UserResponse(
        @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "Registered PAN number", example = "ABCDE1234F")
        String panNumber,

        @Schema(description = "Profile creation timestamp", example = "2026-10-01T01:15:00Z")
        Instant createdAt
) {}
