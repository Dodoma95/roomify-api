package com.roomify.shared.exception.user;

import lombok.Builder;

public class AccountNotVerifiedException extends Exception {
    @Builder
    public AccountNotVerifiedException(String message) {
        super(message);
    }
}
