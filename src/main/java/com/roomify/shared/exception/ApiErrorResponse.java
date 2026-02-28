package com.roomify.shared.exception;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String message,
        String path,
        int status,
        LocalDateTime timestamp,
        String stackTrace
) {
}
