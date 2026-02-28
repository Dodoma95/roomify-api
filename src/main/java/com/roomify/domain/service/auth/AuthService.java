package com.roomify.domain.service.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.api.AuthApi;
import com.roomify.domain.models.event.UserRegisteredEvent;
import com.roomify.domain.spi.EmailVerificationSpi;
import com.roomify.domain.spi.RoleSpi;
import com.roomify.domain.spi.UserSpi;
import com.roomify.infrastucture.models.user.EmailVerificationToken;
import com.roomify.presentation.models.in.LoginRequest;
import com.roomify.presentation.models.out.LoginResponse;
import com.roomify.presentation.models.in.RegisterRequest;
import com.roomify.infrastucture.models.user.Role;
import com.roomify.infrastucture.models.user.User;
import com.roomify.infrastucture.service.JwtService;
import com.roomify.presentation.models.out.RegisterResponse;
import com.roomify.shared.exception.mail.InvalidTokenException;
import com.roomify.shared.exception.mail.TokenNotFoundException;
import com.roomify.shared.exception.user.AccountAlreadyVerifiedException;
import com.roomify.shared.exception.user.AccountNotVerifiedException;
import com.roomify.shared.exception.user.EmailAlreadyExistsException;
import com.roomify.shared.exception.user.RoleNotFoundException;
import com.roomify.shared.exception.user.UserNotFoundException;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import static com.roomify.domain.models.RoleEnum.USER;
import static com.roomify.shared.utils.UtilsRequest.normalizeEmail;
import static com.roomify.shared.utils.VerificationToken.generate;
import static com.roomify.shared.utils.VerificationToken.sha256;

@Service
@Slf4j
public class AuthService implements AuthApi {

    private final UserSpi userSpi;
    private final RoleSpi roleSpi;
    private final EmailVerificationSpi emailVerificationSpi;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserSpi userSpi, RoleSpi roleSpi, EmailVerificationSpi emailVerificationSpi, ApplicationEventPublisher eventPublisher,
            PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userSpi = userSpi;
        this.roleSpi = roleSpi;
        this.emailVerificationSpi = emailVerificationSpi;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    @Override
    @RateLimiter(name = "registerUserRateLimiter")
    public RegisterResponse register(@NonNull RegisterRequest request) throws RoleNotFoundException, EmailAlreadyExistsException {
        controlUniqueEmail(normalizeEmail(request.email()));
        User user = buildUser(request);
        userSpi.insertUser(user);
        processVerificationEmail(user);
        return new RegisterResponse("User registered successfully. Please verify your email before logging in.");
    }

    @Transactional(readOnly = true)
    @Override
    @RateLimiter(name = "loginUserRateLimiter")
    public LoginResponse login(@NonNull LoginRequest request) throws UserNotFoundException, AccountNotVerifiedException {
        User user = getUser(request);
        controlVerifiedEmail(user);
        return buildAuthResponse(user);
    }

    @Transactional
    @Override
    @RateLimiter(name = "registerUserRateLimiter")
    public void verify(@NonNull String token)
            throws InvalidTokenException, TokenNotFoundException, AccountAlreadyVerifiedException {
        String hashed = sha256(token);
        EmailVerificationToken emailVerificationToken = emailVerificationSpi.findEmailVerification(hashed)
                .orElseThrow(() -> TokenNotFoundException.builder()
                        .message("Validation email token not found")
                        .build());

        if (emailVerificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw InvalidTokenException.builder()
                    .message("Validation email token expired")
                    .build();
        }

        User user = emailVerificationToken.getUser();
        controlAlreadyVerifiedEmail(user);

        // Activation user
        user.setEnabled(true);
        user.setEmailVerified(true);
        emailVerificationSpi.deleteEmailVerification(emailVerificationToken);
    }

    @Transactional
    @Override
    @RateLimiter(name = "registerUserRateLimiter")
    public void resendVerification(@NonNull String email) throws UserNotFoundException, AccountAlreadyVerifiedException {
        String emailNormalized = normalizeEmail(email);
        User user = userSpi.findUserByEmail(emailNormalized)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + normalizeEmail(email))
                        .build());
        controlAlreadyVerifiedEmail(user);
        processVerificationEmail(user);
    }

    private void processVerificationEmail(@NonNull User user) {
        String rawToken = generate();
        String hashedToken = sha256(rawToken);
        EmailVerificationToken tokenEntity = buildEmailVerification(hashedToken, user);
        emailVerificationSpi.insertEmailVerification(tokenEntity);
        eventPublisher.publishEvent(
                new UserRegisteredEvent(user.getId(), user.getEmail(), rawToken)
        );
    }

    private User getUser(@NonNull LoginRequest request) throws UserNotFoundException {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizeEmail(request.email()),
                        request.password()
                )
        );

        return (User) Optional.of(authenticate)
                .map(Authentication::getPrincipal)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found with email: " + normalizeEmail(request.email()))
                        .build());
    }

    private User buildUser(@NonNull RegisterRequest request) throws RoleNotFoundException {
        return User.builder()
                .email(normalizeEmail(request.email()))
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(getUserRole()))
                .enabled(true)
                .emailVerified(false)
                .build();
    }

    private EmailVerificationToken buildEmailVerification(@NonNull String hashedToken, @NonNull User user) {
        return EmailVerificationToken.builder()
                .token(hashedToken)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .user(user)
                .build();
    }

    private Role getUserRole() throws RoleNotFoundException {
        return roleSpi.findByName(USER)
                .orElseThrow(() -> RoleNotFoundException.builder()
                        .message("User role not found")
                        .build());
    }

    private LoginResponse buildAuthResponse(@NonNull User user) {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        return new LoginResponse(access, refresh);
    }

    private void controlAlreadyVerifiedEmail(@NonNull User user) throws AccountAlreadyVerifiedException {
        if (user.isEmailVerified()) {
            log.info("Account already verified for: {}", user.getEmail());
            throw AccountAlreadyVerifiedException.builder()
                    .message("Account already verified. Please log in.")
                    .build();
        }
    }

    private void controlVerifiedEmail(@NonNull User user) throws AccountNotVerifiedException {
        if (!user.isEmailVerified()) {
            throw AccountNotVerifiedException.builder()
                    .message("Account not verified. Please verify your email before logging in.")
                    .build();
        }
    }

    private void controlUniqueEmail(@NonNull String email) throws EmailAlreadyExistsException {
        if (userSpi.alreadyExists(email)) {
            throw EmailAlreadyExistsException.builder()
                    .message("Invalid registration request")
                    .build();
        }
    }
}

