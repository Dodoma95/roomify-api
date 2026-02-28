package com.roomify.presentation.models.in;

import org.jspecify.annotations.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ResendVerificationRequest", description = "Request payload to resend verification email")
public record ResendVerificationRequest(
        @Schema(
                description = "User email address",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String email) {
}
