package com.roomify.shared.exception.booking;

import lombok.Builder;

public class BookingPlaceNotApprovedException extends Exception {
    @Builder
    public BookingPlaceNotApprovedException(String message) {
        super(message);
    }
}
