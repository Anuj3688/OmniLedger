package dev.fintech.omniledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Composite payload containing one or more user profile modifications.
 */
@Schema(description = "Request payload for modifying one or more user profile fields")
public record UpdateUserRequest(
        @Schema(description = "List of field modifications to apply atomically", requiredMode = Schema.RequiredMode.REQUIRED)
        List<UserModificationItem> modifications
) {}
