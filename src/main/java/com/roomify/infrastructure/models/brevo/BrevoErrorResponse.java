package com.roomify.infrastructure.models.brevo;

public record BrevoErrorResponse(
        String code,
        String message
) {}
