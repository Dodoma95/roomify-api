package com.roomify.presentation.models.in;

import org.jspecify.annotations.NonNull;

import com.roomify.shared.utils.ValidationPatterns;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "Request payload to register a new user")
public record RegisterRequest(
        @Schema(
                description = "User first name",
                example = "John",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 2, max = 100)
        @Pattern(
                regexp = ValidationPatterns.NAME,
                message = "Invalid first name"
        )
        @NonNull String firstName,

        @Schema(
                description = "User last name",
                example = "Doe",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 2, max = 100)
        @Pattern(
                regexp = ValidationPatterns.NAME,
                message = "Invalid last name"
        )
        @NonNull String lastName,

        @Schema(
                description = "User email address",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        @Size(max = 255)
        @NonNull String email,

        @Schema(
                description = "User password",
                example = "P@ssword123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 12, max = 100)
        @Pattern(
                regexp = ValidationPatterns.PASSWORD,
                message = "Password must contain upper, lower, digit and special character"
        )
        @NonNull String password
) {
}
