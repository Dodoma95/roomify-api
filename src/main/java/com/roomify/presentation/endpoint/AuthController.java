package com.roomify.presentation.endpoint;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.roomify.domain.api.AuthApi;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.LoginRequest;
import com.roomify.presentation.models.in.ResendVerificationRequest;
import com.roomify.presentation.models.out.LoginResponse;
import com.roomify.presentation.models.in.RegisterRequest;
import com.roomify.presentation.models.out.RegisterResponse;
import com.roomify.presentation.models.out.UserResponse;
import com.roomify.shared.exception.ClientApiException;
import com.roomify.shared.exception.mail.InvalidTokenException;
import com.roomify.shared.exception.mail.TokenNotFoundException;
import com.roomify.shared.exception.user.AccountAlreadyVerifiedException;
import com.roomify.shared.exception.user.AccountNotVerifiedException;
import com.roomify.shared.exception.user.EmailAlreadyExistsException;
import com.roomify.shared.exception.user.RoleNotFoundException;
import com.roomify.shared.exception.user.UserNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authentication", description = "Authentication and JWT management")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApi authApi;

    public AuthController(AuthApi authApi) {
        this.authApi = authApi;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with USER role"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User successfully registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterResponse.class)
                    )),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content),
            @ApiResponse(
                    responseCode = "403",
                    description = "Request forbidden",
                    content = @Content),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many request",
                    content = @Content),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authApi.register(request));
        } catch (RoleNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (EmailAlreadyExistsException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Login an existing user",
            description = "Login an existing user account with ANY role"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully login",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class)
                    )),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content),
            @ApiResponse(
                    responseCode = "403",
                    description = "Request forbidden",
                    content = @Content),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many request",
                    content = @Content),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        try {
            return ResponseEntity.ok(authApi.login(request));
        } catch (UserNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (AccountNotVerifiedException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Verify email token",
            description = "Activate a user account using verification token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Account successfully verified",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Token not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Token already used",
                    content = @Content
            )
    })
    @GetMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam("token") String token) {
        try {
            authApi.verify(token);
            return ResponseEntity.noContent().build();
        } catch (InvalidTokenException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        } catch (TokenNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (AccountAlreadyVerifiedException e) {
            throw ClientApiException.ofConflict(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Resend verification email",
            description = "Send a new verification email if account is not yet verified"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Verification email sent",
                    content = @Content),
            @ApiResponse(
                    responseCode = "400",
                    description = "Account already verified",
                    content = @Content)
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ResendVerificationRequest request) {
        try {
            authApi.resendVerification(request.email());
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.noContent().build();
        } catch (AccountAlreadyVerifiedException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Get current authenticated user",
            description = "Returns information about the currently authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authenticated user information successfully retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many requests",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        // TODO j'en suis la
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                new UserResponse(
                        user.getEmail(),
                        user.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()
                )
        );
    }

}
