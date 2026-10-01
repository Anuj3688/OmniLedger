package dev.fintech.omniledger.dto.request;

import dev.fintech.omniledger.model.enums.ResidentialStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Payload for onboarding a natural person (retail individual customer).
 * Captures residential status under FEMA (Resident Indian vs. NRI).
 */
@Schema(description = "Payload for onboarding a natural person (retail customer)")
public record CreateIndividualPartyRequest(
        @Schema(description = "Customer legal full name", example = "Rahul Sharma", requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,

        @Schema(description = "Customer email address", example = "rahul.sharma@example.com")
        String email,

        @Schema(description = "Contact phone number", example = "+919876543210")
        String phoneNumber,

        @Schema(description = "Permanent Account Number (PAN)", example = "ABCDE1234F", requiredMode = Schema.RequiredMode.REQUIRED)
        String panNumber,

        @Schema(description = "Residential status under FEMA", example = "RESIDENT_INDIAN", defaultValue = "RESIDENT_INDIAN")
        ResidentialStatus residentialStatus,

        @Schema(description = "ISO 3166-1 alpha-2 country of residence", example = "IN", defaultValue = "IN")
        String countryOfResidence,

        @Schema(description = "Date of birth (YYYY-MM-DD)", example = "1990-05-15")
        LocalDate dateOfBirth
) {
    public CreateIndividualPartyRequest {
        if (residentialStatus == null) {
            residentialStatus = ResidentialStatus.RESIDENT_INDIAN;
        }
        if (countryOfResidence == null || countryOfResidence.isBlank()) {
            countryOfResidence = "IN";
        }
    }
}
