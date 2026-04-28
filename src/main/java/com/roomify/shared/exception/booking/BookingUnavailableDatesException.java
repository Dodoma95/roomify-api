package com.roomify.shared.exception.booking;

import lombok.Builder;

public class BookingUnavailableDatesException extends Exception {
    @Builder
    public BookingUnavailableDatesException(String message) {
        super(message);
    }
}
