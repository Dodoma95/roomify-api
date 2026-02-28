package com.roomify.shared.exception.user;

import lombok.Builder;

public class UserNotFoundException extends Exception {
    @Builder
    public UserNotFoundException(String message) {
        super(message);
    }
}
