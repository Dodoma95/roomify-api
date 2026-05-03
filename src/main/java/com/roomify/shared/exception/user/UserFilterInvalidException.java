package com.roomify.shared.exception.user;

import com.roomify.shared.exception.FilterInvalidException;

import lombok.Builder;

public class UserFilterInvalidException extends FilterInvalidException {
    @Builder
    public UserFilterInvalidException(String message) {
        super(message);
    }
}
