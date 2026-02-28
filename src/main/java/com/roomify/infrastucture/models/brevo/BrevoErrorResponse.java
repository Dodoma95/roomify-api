package com.roomify.infrastucture.models.brevo;

public record BrevoErrorResponse(
        String code,
        String message
) {}
