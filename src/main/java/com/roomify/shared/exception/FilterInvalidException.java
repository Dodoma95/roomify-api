package com.roomify.shared.exception;

public abstract class FilterInvalidException extends RuntimeException {
    protected FilterInvalidException(String message) {
        super(message);
    }
}
