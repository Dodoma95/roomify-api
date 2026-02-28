package com.roomify.shared.exception.user;

import lombok.Builder;

public class RoleNotFoundException extends Exception {
    @Builder
    public RoleNotFoundException(String message) {
        super(message);
    }
}
