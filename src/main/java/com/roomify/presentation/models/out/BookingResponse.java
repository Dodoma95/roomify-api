package com.roomify.presentation.models.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BookingResponse", description = "Represents a booking for a place")
public record BookingResponse(

        @Schema(description = "Unique identifier of the booking", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull Long id,

        @Schema(description = "ID of the booked place", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull Long placeId,

        @Schema(description = "Name of the booked place", example = "Modern Meeting Room", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String placeName,

        @Schema(description = "ID of the user who booked", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull Long userId,

        @Schema(description = "First name of the user who booked", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String userFirstName,

        @Schema(description = "Last name of the user who booked", example = "Dupont", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String userLastName,

        @Schema(description = "First day of the booking (inclusive)", example = "2025-07-10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull LocalDate startDate,

        @Schema(description = "Last day of the booking (inclusive)", example = "2025-07-15", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull LocalDate endDate,

        @Schema(description = "Status of the booking", example = "CONFIRMED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String status,

        @Schema(description = "Total price in euros (days × price per hour)", example = "125.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull BigDecimal totalPrice,

        @Schema(description = "Description of the user who booked", example = "Product designer based in Paris")
        @Nullable String userDescription,

        @Schema(description = "Avatar URL of the user who booked", example = "https://cdn.example.com/avatars/jean.jpg")
        @Nullable String userAvatarUrl,

        @Schema(description = "Optional notes from the user", example = "We will need a projector setup")
        @Nullable String notes,

        @Schema(description = "Booking creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull LocalDateTime createdAt
) {}
