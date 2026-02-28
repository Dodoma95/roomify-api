package com.roomify.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

import static java.util.Objects.nonNull;

@Getter
public class ClientApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    private ClientApiException(String message, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = nonNull(statusCode) ? statusCode : HttpStatus.BAD_REQUEST;
    }

    public static ClientApiException ofBadRequest(String message, Throwable cause) {
        return ClientApiException.builder()
                .message(message)
                .cause(cause)
                .statusCode(HttpStatus.BAD_REQUEST)
                .build();
    }

    public static ClientApiException ofForbidden(String message, Throwable cause) {
        return ClientApiException.builder()
                .message(message)
                .cause(cause)
                .statusCode(HttpStatus.FORBIDDEN)
                .build();
    }

    public static ClientApiException ofConflict(String message, Throwable cause) {
        return ClientApiException.builder()
                .message(message)
                .cause(cause)
                .statusCode(HttpStatus.CONFLICT)
                .build();
    }

    public static ClientApiException ofUnauthorized(String message, Throwable cause) {
        return ClientApiException.builder()
                .message(message)
                .cause(cause)
                .statusCode(HttpStatus.UNAUTHORIZED)
                .build();
    }

    public static ClientApiException ofNotFound(String message, Throwable cause) {
        return ClientApiException.builder()
                .message(message)
                .cause(cause)
                .statusCode(HttpStatus.NOT_FOUND)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private HttpStatusCode statusCode;
        private Throwable cause;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder statusCode(HttpStatusCode statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public ClientApiException build() {
            return new ClientApiException(message, statusCode, cause);
        }
    }
}
