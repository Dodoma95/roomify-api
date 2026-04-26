package com.roomify.shared.exception.place;

import lombok.Builder;

public class PlaceFilterInvalidException extends RuntimeException {
    @Builder
    public PlaceFilterInvalidException(String message) {
        super(message);
    }
}
