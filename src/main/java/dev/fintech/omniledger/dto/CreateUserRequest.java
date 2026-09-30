package dev.fintech.omniledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload for onboarding a new user profile with tax identification.
 */
@Schema(description = "Payload for onboarding a new customer")
public record CreateUserRequest(
        @Schema(description = "Permanent Account Number (PAN)", example = "ABCDE1234F", requiredMode = Schema.RequiredMode.REQUIRED)
        String panNumber
) {}
