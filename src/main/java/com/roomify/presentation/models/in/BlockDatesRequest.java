package com.roomify.presentation.models.in;

import java.time.LocalDate;

import org.jspecify.annotations.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

@Schema(name = "BlockDatesRequest", description = "Request payload to block dates on a place")
public record BlockDatesRequest(

        @Schema(description = "First day to block (inclusive)", example = "2025-08-01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @FutureOrPresent
        @NonNull LocalDate startDate,

        @Schema(description = "Last day to block (inclusive)", example = "2025-08-05", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Future
        @NonNull LocalDate endDate
) {}
