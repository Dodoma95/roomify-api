package com.roomify.presentation.models.out;

import java.time.Instant;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.roomify.domain.models.RoleEnum;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserAdminResponse", description = "User details visible to administrators")
public record UserAdminResponse(

        @Schema(description = "User identifier", example = "42")
        @NonNull Long id,

        @Schema(description = "User email address", example = "alice.martin@example.com")
        @NonNull String email,

        @Schema(description = "User first name", example = "Alice")
        @NonNull String firstName,

        @Schema(description = "User last name", example = "Martin")
        @NonNull String lastName,

        @Schema(description = "Roles assigned to the user", example = "[\"USER\", \"OWNER\"]")
        @NonNull Set<RoleEnum> roles,

        @Schema(description = "Whether the account is enabled", example = "true")
        boolean enabled,

        @Schema(description = "Whether the email address has been verified", example = "true")
        boolean emailVerified,

        @Schema(description = "Soft-deletion timestamp, null if the user is active", example = "null")
        @Nullable Instant deletedAt,

        @Schema(description = "Id of the admin who performed the soft-deletion", example = "null")
        @Nullable Long deletedBy
) {}
