package com.roomify.shared.exception.booking;

import lombok.Builder;

public class BookingOwnPlaceException extends Exception {
    @Builder
    public BookingOwnPlaceException(String message) {
        super(message);
    }
}
