package dev.fintech.omniledger.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Filter payload for querying system events within a specific time window.
 */
@Schema(description = "Time-range filter for querying system events")
public record EventFilterRequest(
        @Schema(description = "Start of time range (UTC Instant)", example = "2026-10-01T00:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant startTime,

        @Schema(description = "End of time range (UTC Instant)", example = "2026-10-01T23:59:59Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant endTime
) {}
