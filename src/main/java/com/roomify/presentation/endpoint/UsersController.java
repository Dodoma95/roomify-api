package com.roomify.presentation.endpoint;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.roomify.domain.api.UserApi;
import com.roomify.infrastucture.models.user.CustomUserDetails;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.UpdateMeRequest;
import com.roomify.presentation.models.out.UserResponse;
import com.roomify.shared.exception.ClientApiException;
import com.roomify.shared.exception.user.UserActionForbiddenException;
import com.roomify.shared.exception.user.UserNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Users", description = "Management users")
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UserApi userApi;

    public UsersController(UserApi userApi) {
        this.userApi = userApi;
    }

    @Operation(
            summary = "Get current authenticated user",
            description = "Returns information about the currently authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "Authenticated user information successfully retrieved",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid JWT token",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions",
            content = @Content
    )
    @ApiResponse(
            responseCode = "429",
            description = "Too many requests",
            content = @Content
    )
    @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content
    )
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails customUser) {
        User user = customUser.user();
        return ResponseEntity.ok(
                new UserResponse(
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()
                )
        );
    }

    @Operation(
            summary = "Update current user",
            description = "Partially updates the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "User successfully updated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)
            ))
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content)
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content)
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal CustomUserDetails customUser,
            @RequestBody @Valid UpdateMeRequest request
    ) {
        try {
            return ResponseEntity.ok(userApi.updateMe(customUser.user(), request));
        } catch (UserNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Delete a user",
            description = "Deletes a user by id (soft delete). Only ADMIN or SUPER_ADMIN can perform this action",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "204",
            description = "User successfully deleted",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid JWT token",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal CustomUserDetails customUser) {
        try {
            userApi.deleteMe(customUser.user());
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Delete a user",
            description = "Deletes a user by id (soft delete). Only ADMIN or SUPER_ADMIN can perform this action",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "204",
            description = "User successfully deleted",
            content = @Content
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid JWT token",
            content = @Content
    )
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            userApi.deleteUserById(id, currentUser.user());
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        }
    }

}
