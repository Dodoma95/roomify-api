package com.roomify.presentation.models.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.roomify.domain.models.BookingStatusEnum;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BookingGraphQLResponse", description = "Full booking details including place and user info")
public record BookingGraphQLResponse(

        @Schema(description = "Booking identifier", example = "1")
        @NonNull Long id,

        @Schema(description = "Booking start date (YYYY-MM-DD)", example = "2025-06-01")
        @NonNull LocalDate startDate,

        @Schema(description = "Booking end date (YYYY-MM-DD)", example = "2025-06-05")
        @NonNull LocalDate endDate,

        @Schema(description = "Current booking status", example = "CONFIRMED")
        @NonNull BookingStatusEnum status,

        @Schema(description = "Total price in euros", example = "200.00")
        @NonNull BigDecimal totalPrice,

        @Schema(description = "Optional notes from the tenant", example = "Need projector access")
        @Nullable String notes,

        @Schema(description = "Booking creation timestamp (ISO-8601)", example = "2025-01-15T10:30:00Z")
        @NonNull Instant createdAt,

        @Schema(description = "Place information")
        @NonNull BookingPlaceInfo place,

        @Schema(description = "User who made the booking")
        @NonNull OwnerInfo booker
) {}
