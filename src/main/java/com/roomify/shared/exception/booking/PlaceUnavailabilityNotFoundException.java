package com.roomify.shared.exception.booking;

import lombok.Builder;

public class PlaceUnavailabilityNotFoundException extends Exception {
    @Builder
    public PlaceUnavailabilityNotFoundException(String message) {
        super(message);
    }
}
