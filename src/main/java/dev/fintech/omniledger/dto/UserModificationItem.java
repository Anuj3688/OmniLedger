package dev.fintech.omniledger.dto;

import dev.fintech.omniledger.model.enums.UserModificationType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single field modification instruction within a profile update request.
 */
@Schema(description = "Individual user field modification item")
public record UserModificationItem(
        @Schema(description = "Target field to update", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
        UserModificationType type,

        @Schema(description = "New value for the field", example = "rahul.new@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String value
) {}
