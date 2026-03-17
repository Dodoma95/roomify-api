package com.roomify.presentation.endpoint;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.roomify.infrastructure.models.user.CustomUserDetails;
import com.roomify.infrastructure.models.user.User;
import com.roomify.presentation.models.out.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users", description = "Management users")
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

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
                        user.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()
                )
        );
    }

}
