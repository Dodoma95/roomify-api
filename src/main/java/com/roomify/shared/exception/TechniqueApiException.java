package com.roomify.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public class TechniqueApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public TechniqueApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
