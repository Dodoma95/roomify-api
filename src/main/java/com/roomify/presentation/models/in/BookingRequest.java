package com.roomify.presentation.models.in;

import java.time.LocalDate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "BookingRequest", description = "Request payload to book a place")
public record BookingRequest(

        @Schema(description = "ID of the place to book", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @NonNull Long placeId,

        @Schema(description = "First day of the booking (inclusive)", example = "2025-07-10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @FutureOrPresent
        @NonNull LocalDate startDate,

        @Schema(description = "Last day of the booking (inclusive)", example = "2025-07-15", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Future
        @NonNull LocalDate endDate,

        @Schema(description = "Optional notes for the owner", example = "We will need a projector setup")
        @Size(max = 500)
        @Nullable String notes
) {}
