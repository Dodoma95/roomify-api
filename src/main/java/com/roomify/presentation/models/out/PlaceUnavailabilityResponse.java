package com.roomify.presentation.models.out;

import java.time.LocalDate;

import org.jspecify.annotations.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PlaceUnavailabilityResponse", description = "Represents an unavailability period for a place")
public record PlaceUnavailabilityResponse(

        @Schema(description = "Unique identifier", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull Long id,

        @Schema(description = "ID of the place", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull Long placeId,

        @Schema(description = "First blocked day (inclusive)", example = "2025-07-10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull LocalDate startDate,

        @Schema(description = "Last blocked day (inclusive)", example = "2025-07-15", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull LocalDate endDate,

        @Schema(description = "Reason: BOOKING or OWNER_BLOCKED", example = "BOOKING", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String reason
) {}
