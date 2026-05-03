package com.roomify.presentation.models.in;

import org.jspecify.annotations.Nullable;

import com.roomify.domain.models.RoleEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFilterInput {

    @Schema(description = "Filter users whose first name contains this value (case-insensitive)", example = "ali")
    @Nullable String firstNameContains;

    @Schema(description = "Filter users whose last name contains this value (case-insensitive)", example = "du")
    @Nullable String lastNameContains;

    @Schema(description = "Filter users whose email contains this value (case-insensitive)", example = "@gmail")
    @Nullable String emailContains;

    @Schema(description = "Filter users by role", example = "OWNER")
    @Nullable RoleEnum role;

    @Schema(description = "true = soft-deleted only, false = active only, omit = all users", example = "false")
    @Nullable Boolean deleted;

    @Schema(description = "Filter by account enabled status", example = "true")
    @Nullable Boolean enabled;

    @Schema(description = "Filter by email verification status", example = "true")
    @Nullable Boolean emailVerified;
}
