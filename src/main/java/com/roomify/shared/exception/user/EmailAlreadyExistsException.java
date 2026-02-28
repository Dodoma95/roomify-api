package com.roomify.shared.exception.user;

import lombok.Builder;

public class EmailAlreadyExistsException extends Exception {
    @Builder
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
