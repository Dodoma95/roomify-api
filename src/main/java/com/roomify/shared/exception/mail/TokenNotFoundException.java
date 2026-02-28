package com.roomify.shared.exception.mail;

import lombok.Builder;
import lombok.Getter;

@Getter
public class TokenNotFoundException extends Exception {
    @Builder
    public TokenNotFoundException(String message) {
        super(message);
    }
}
