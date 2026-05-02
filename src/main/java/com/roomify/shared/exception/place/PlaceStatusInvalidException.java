package com.roomify.shared.exception.place;

import lombok.Builder;

public class PlaceStatusInvalidException extends Exception {
    @Builder
    public PlaceStatusInvalidException(String message) {
        super(message);
    }
}
