package com.roomify.presentation.models.out;

import org.jspecify.annotations.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Register response containing confirmation message")
public record RegisterResponse(
        @Schema(description = "Message de confirmation de l'inscription")
        @NonNull String message
) {
}
