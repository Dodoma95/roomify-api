package com.roomify.presentation.models.in;

import com.roomify.shared.utils.ValidationPatterns;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UpdateMeRequest(
        @Schema(description = "User first name", example = "John")
        @Size(min = 2, max = 100)
        @Pattern(regexp = ValidationPatterns.NAME, message = "Invalid first name")
        @Nullable String firstName,

        @Schema(description = "User last name", example = "Doe")
        @Size(min = 2, max = 100)
        @Pattern(regexp = ValidationPatterns.NAME, message = "Invalid last name")
        @Nullable String lastName,

        @Schema(description = "User email address", example = "user@example.com")
        @Email
        @Size(max = 255)
        @Nullable String email,

        @Schema(description = "User profile description", example = "Passionné de voyage et de randonnée.")
        @Size(max = 1000)
        @Nullable String description
) {}
