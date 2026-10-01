package dev.fintech.omniledger.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload for onboarding a commercial merchant or enterprise legal entity.
 * Supports domestic Indian entities and foreign corporations.
 */
@Schema(description = "Payload for onboarding a commercial merchant or corporate entity")
public record CreateBusinessPartyRequest(
        @Schema(description = "Official registered corporate legal name", example = "Acme Retail Technologies Pvt Ltd", requiredMode = Schema.RequiredMode.REQUIRED)
        String legalBusinessName,

        @Schema(description = "Commercial or trade brand name", example = "AcmePay")
        String tradeName,

        @Schema(description = "ISO 3166-1 alpha-2 country of incorporation", example = "IN", defaultValue = "IN")
        String countryOfIncorporation,

        @Schema(description = "Goods and Services Tax Identification Number (mandatory for Indian businesses)", example = "27ABCDE1234F1Z5")
        String gstin,

        @Schema(description = "Corporate Identification Number (for Indian companies)", example = "U72900MH2020PTC123456")
        String cinNumber,

        @Schema(description = "Corporate PAN (mandatory for Indian businesses)", example = "ABCDE1234F")
        String corporatePan,

        @Schema(description = "Foreign entity registration number / EIN / LEI (mandatory for foreign businesses)", example = "US-EIN-12-3456789")
        String foreignRegistrationNumber,

        @Schema(description = "Corporate billing or contact email", example = "billing@acmepay.com")
        String email,

        @Schema(description = "Corporate desk contact number", example = "+912212345678")
        String phoneNumber
) {
    public CreateBusinessPartyRequest {
        if (countryOfIncorporation == null || countryOfIncorporation.isBlank()) {
            countryOfIncorporation = "IN";
        }
    }
}
