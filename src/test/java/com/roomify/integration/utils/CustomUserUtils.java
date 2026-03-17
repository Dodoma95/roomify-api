package com.roomify.integration.utils;

import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.roomify.infrastructure.models.user.CustomUserDetails;
import com.roomify.infrastructure.models.user.Role;
import com.roomify.infrastructure.models.user.User;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CustomUserUtils {

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

}
