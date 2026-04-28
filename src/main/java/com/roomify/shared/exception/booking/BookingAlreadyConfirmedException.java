package com.roomify.shared.exception.booking;

import lombok.Builder;

public class BookingAlreadyConfirmedException extends Exception {
    @Builder
    public BookingAlreadyConfirmedException(String message) {
        super(message);
    }
}
