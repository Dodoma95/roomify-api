package com.roomify.shared.exception.user;

import lombok.Builder;

public class RoleAlreadyAssignedException extends Exception {
    @Builder
    public RoleAlreadyAssignedException(String message) {
        super(message);
    }
}
