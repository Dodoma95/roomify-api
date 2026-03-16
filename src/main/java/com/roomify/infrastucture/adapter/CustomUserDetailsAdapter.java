package com.roomify.infrastucture.adapter;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.roomify.infrastucture.repository.UserRepository;

@Service
public class CustomUserDetailsAdapter implements UserDetailsService {

    private final UserRepository repository;

    public CustomUserDetailsAdapter(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}

