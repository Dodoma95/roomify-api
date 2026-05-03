package com.roomify.shared.exception.user;

import lombok.Builder;

public class RoleNotAssignedException extends Exception {
    @Builder
    public RoleNotAssignedException(String message) {
        super(message);
    }
}
