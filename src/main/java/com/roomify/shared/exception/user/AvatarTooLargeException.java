package com.roomify.shared.exception.user;

import lombok.Builder;

public class AvatarTooLargeException extends Exception {

    @Builder
    public AvatarTooLargeException(String message) {
        super(message);
    }
}
