package com.roomify.presentation.models.out;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserResponse", description = "Authenticated user information")
public record UserResponse(
        @Schema(description = "User unique identifier", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull Long id,

        @Schema(description = "User email address", example = "john.doe@roomify.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String email,

        @Schema(description = "User first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String firstName,

        @Schema(description = "User last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull String lastName,

        @Schema(description = "List of granted roles", example = "[\"ROLE_USER\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NonNull List<String> roles,

        @Schema(description = "Profile description", example = "Passionné de voyage.")
        @Nullable String description,

        @Schema(description = "Profile picture public URL", example = "https://pub-xxx.r2.dev/avatars/42/uuid")
        @Nullable String avatarUrl
) {}
