package dev.fintech.omniledger.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload for onboarding a sovereign tax agency or municipal department.
 */
@Schema(description = "Payload for onboarding a sovereign tax authority or government agency")
public record CreateGovernmentPartyRequest(
        @Schema(description = "Official department name", example = "Central Board of Indirect Taxes and Customs", requiredMode = Schema.RequiredMode.REQUIRED)
        String departmentName,

        @Schema(description = "Jurisdictional authority scope", example = "CENTRAL", requiredMode = Schema.RequiredMode.REQUIRED)
        String jurisdiction,

        @Schema(description = "Treasury nodal agency identification code", example = "CBIC-NODAL-01", requiredMode = Schema.RequiredMode.REQUIRED)
        String nodalAgencyCode,

        @Schema(description = "Category of tax or levy collected", example = "GST", requiredMode = Schema.RequiredMode.REQUIRED)
        String taxCategoryCode
) {}
