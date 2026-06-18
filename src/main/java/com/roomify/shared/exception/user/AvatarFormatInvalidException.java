package com.roomify.shared.exception.user;

import lombok.Builder;

public class AvatarFormatInvalidException extends Exception {

    @Builder
    public AvatarFormatInvalidException(String message) {
        super(message);
    }
}
