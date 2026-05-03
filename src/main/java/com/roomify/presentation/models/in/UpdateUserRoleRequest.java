package com.roomify.presentation.models.in;

import org.jspecify.annotations.NonNull;

import com.roomify.domain.models.RoleActionEnum;
import com.roomify.domain.models.RoleEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateUserRoleRequest", description = "Request to add or remove a role from a user")
public record UpdateUserRoleRequest(

        @Schema(description = "Role to add or remove", example = "OWNER")
        @NotNull
        @NonNull RoleEnum role,

        @Schema(description = "Action to perform on the role", example = "ADD")
        @NotNull
        @NonNull RoleActionEnum action
) {}
