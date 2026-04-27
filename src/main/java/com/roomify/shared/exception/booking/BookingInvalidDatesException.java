package com.roomify.shared.exception.booking;

import lombok.Builder;

public class BookingInvalidDatesException extends Exception {
    @Builder
    public BookingInvalidDatesException(String message) {
        super(message);
    }
}
