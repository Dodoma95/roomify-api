package com.roomify.presentation.models.out;

import java.math.BigDecimal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PlaceResponse",
        description = "Represents a place created by a user"
)
public record PlaceResponse(
        @Schema(
                description = "Unique identifier of the place",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull Long id,

        @Schema(
                description = "Name of the place",
                example = "Modern meeting room in city center",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull String name,

        @Schema(
                description = "Detailed description of the place",
                example = "A fully equipped meeting room with projector and high-speed Wi-Fi"
        )
        @Nullable String description,

        @Schema(
                description = "Type of the place",
                example = "MEETING_ROOM",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull String type,

        @Schema(
                description = "Address of the place",
                example = "10 rue de Paris, 75001 Paris",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull String address,

        @Schema(
                description = "Maximum capacity of the place",
                example = "10"
        )
        @Nullable Integer capacity,

        @Schema(
                description = "Price per hour in euros",
                example = "25.50"
        )
        @Nullable BigDecimal pricePerHour,

        @Schema(
                description = "Validation status of the place",
                example = "PENDING",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull String status,

        @Schema(description = "Owner of the place")
        @Nullable OwnerInfo owner
) {}
