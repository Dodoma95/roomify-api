package com.roomify.presentation.models.in;

import com.roomify.shared.utils.ValidationPatterns;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Schema(
                description = "User first name",
                example = "John")
        @Size(min = 2, max = 100)
        @Pattern(
                regexp = ValidationPatterns.NAME,
                message = "Invalid first name"
        )
        String firstName,

        @Schema(
                description = "User last name",
                example = "Doe")
        @Size(min = 2, max = 100)
        @Pattern(
                regexp = ValidationPatterns.NAME,
                message = "Invalid last name"
        )
        String lastName,

        @Schema(
                description = "User email address",
                example = "user@example.com")
        @Email
        @Size(max = 255)
        String email
) {}
