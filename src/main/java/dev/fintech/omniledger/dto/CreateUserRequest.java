package dev.fintech.omniledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload for onboarding a new customer with identity, contact, and tax details.
 */
@Schema(description = "Payload for onboarding a new customer")
public record CreateUserRequest(
        @Schema(description = "Customer legal full name", example = "Rahul Sharma")
        String fullName,

        @Schema(description = "Customer email address", example = "rahul.sharma@example.com")
        String email,

        @Schema(description = "Contact phone number", example = "+919876543210")
        String phoneNumber,

        @Schema(description = "Permanent Account Number (PAN)", example = "ABCDE1234F", requiredMode = Schema.RequiredMode.REQUIRED)
        String panNumber
) {}
