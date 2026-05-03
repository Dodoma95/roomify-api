package com.roomify.shared.exception.place;

import com.roomify.shared.exception.FilterInvalidException;

import lombok.Builder;

public class PlaceFilterInvalidException extends FilterInvalidException {
    @Builder
    public PlaceFilterInvalidException(String message) {
        super(message);
    }
}
