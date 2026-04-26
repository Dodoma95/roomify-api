package com.roomify.shared.exception.place;

import lombok.Builder;

public class PlaceNotFoundException extends Exception {
    @Builder
    public PlaceNotFoundException(String message) {
        super(message);
    }
}
