package com.roomify.domain.models.event;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String token
) {}
