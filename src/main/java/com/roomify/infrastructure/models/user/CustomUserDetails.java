package com.roomify.infrastructure.models.user;

import java.util.Collection;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record CustomUserDetails(User user) implements UserDetails {

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public @NonNull String getUsername() {
        return user.getEmail();
    }
}
