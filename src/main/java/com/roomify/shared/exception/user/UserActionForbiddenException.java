package com.roomify.shared.exception.user;

import lombok.Builder;

public class UserActionForbiddenException extends Exception {
    @Builder
    public UserActionForbiddenException(String message) {
        super(message);
    }
}
