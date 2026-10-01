package dev.fintech.omniledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard response payload representing a customer identity profile.
 */
@Schema(description = "Customer profile and identification details")
public record UserResponse(
        @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "Customer legal full name", example = "Rahul Sharma")
        String fullName,

        @Schema(description = "Customer email address", example = "rahul.sharma@example.com")
        String email,

        @Schema(description = "Contact phone number", example = "+919876543210")
        String phoneNumber,

        @Schema(description = "Registered PAN number", example = "ABCDE1234F")
        String panNumber,

        @Schema(description = "Profile creation timestamp", example = "2026-10-01T01:15:00Z")
        Instant createdAt,

        @Schema(description = "Profile last update timestamp", example = "2026-10-01T01:15:00Z")
        Instant updatedAt
) {}
