package com.roomify.presentation.models.out;

import java.util.List;

import org.jspecify.annotations.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "UserResponse",
        description = "Authenticated user information"
)
public record UserResponse(
        @Schema(
                description = "User email address",
                example = "john.doe@roomify.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull String email,
        @Schema(
                description = "User first name",
                example = "John",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull String firstName,
        @Schema(
                description = "User last name",
                example = "Doe",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull String lastName,
        @Schema(
                description = "List of granted roles associated with the user",
                example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NonNull List<String> roles
) {
}
