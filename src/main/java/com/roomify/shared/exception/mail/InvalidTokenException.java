package com.roomify.shared.exception.mail;

import lombok.Builder;
import lombok.Getter;

@Getter
public class InvalidTokenException extends RuntimeException {
    @Builder
    public InvalidTokenException(String message) {
        super(message);
    }
}
