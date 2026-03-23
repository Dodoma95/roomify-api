package com.roomify.integration.utils;

import java.time.LocalDateTime;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.roomify.infrastucture.models.user.CustomUserDetails;
import com.roomify.infrastucture.models.user.EmailVerificationToken;
import com.roomify.infrastucture.models.user.Role;
import com.roomify.infrastucture.models.user.User;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserUtils {

    public static CustomUserDetails createCustomUserDetails(
            @NonNull String email,
            @NonNull String password,
            @NonNull Set<Role> roles) {
        return new CustomUserDetails(User.builder()
                .email(email)
                .password(password)
                .roles(roles)
                .build());
    }

    public static User createUser(
            @NonNull String email,
            @NonNull String password,
            boolean enabled,
            boolean emailVerified
    ) {
        return User.builder()
                .email(email)
                .password(password)
                .enabled(enabled)
                .emailVerified(emailVerified)
                .build();
    }

    public static EmailVerificationToken createEmailVerificationToken(
            @NonNull String token,
            @NonNull User user,
            @NonNull LocalDateTime expiresAt
    ) {
        return EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .build();
    }

}
