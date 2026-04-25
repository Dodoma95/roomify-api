package com.roomify.presentation.models.in;

import java.math.BigDecimal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.roomify.domain.models.PlaceTypeEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        name = "CreatePlaceRequest",
        description = "Request payload to create a new place"
)
public record PlaceRequest(
        @Schema(
                description = "Name of the place",
                example = "Modern meeting room in city center",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(min = 3, max = 150)
        @NonNull String name,

        @Schema(
                description = "Detailed description of the place",
                example = "A fully equipped meeting room with projector and high-speed Wi-Fi"
        )
        @Size(max = 1000)
        @Nullable String description,

        @Schema(
                description = "Type of the place",
                example = "MEETING_ROOM",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        @NonNull PlaceTypeEnum type,

        @Schema(
                description = "Address of the place",
                example = "10 rue de Paris, 75001 Paris",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 255)
        @NonNull String address,

        @Schema(
                description = "Maximum capacity of the place",
                example = "10"
        )
        @Min(1)
        @Max(1000)
        @Nullable Integer capacity,

        @Schema(
                description = "Price per hour in euros",
                example = "25.50",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        @NonNull BigDecimal pricePerHour
) {}
