package com.roomify.presentation.models.in;

import org.jspecify.annotations.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest", description = "Request payload to log in a user")
public record LoginRequest(
        @Schema(
                description = "User email address",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String email,
        @Schema(
                description = "User password",
                example = "P@ssword123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String password
) {
}
