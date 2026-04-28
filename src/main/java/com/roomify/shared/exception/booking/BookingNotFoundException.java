package com.roomify.shared.exception.booking;

import lombok.Builder;

public class BookingNotFoundException extends Exception {
    @Builder
    public BookingNotFoundException(String message) {
        super(message);
    }
}
