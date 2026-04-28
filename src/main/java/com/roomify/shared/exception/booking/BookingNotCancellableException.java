package com.roomify.shared.exception.booking;

import lombok.Builder;

public class BookingNotCancellableException extends Exception {
    @Builder
    public BookingNotCancellableException(String message) {
        super(message);
    }
}
