package com.roomify.shared.exception.user;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AccountAlreadyVerifiedException extends Exception {
    @Builder
    public AccountAlreadyVerifiedException(String message) {
        super(message);
    }
}
