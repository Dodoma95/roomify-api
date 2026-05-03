package com.roomify.shared.exception.booking;

import com.roomify.shared.exception.FilterInvalidException;

import lombok.Builder;

public class BookingFilterInvalidException extends FilterInvalidException {
    @Builder
    public BookingFilterInvalidException(String message) {
        super(message);
    }
}
