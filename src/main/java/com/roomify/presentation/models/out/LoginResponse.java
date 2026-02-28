package com.roomify.presentation.models.out;

import org.jspecify.annotations.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT tokens")
public record LoginResponse(
        @Schema(
                description = "Access token (valid 15 minutes)",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @NonNull String accessToken,
        @Schema(
                description = "Refresh token (valid 7 days)",
                example = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4=")
        @NonNull String refreshToken
) {
}
