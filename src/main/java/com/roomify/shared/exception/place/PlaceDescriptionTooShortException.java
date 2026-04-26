package com.roomify.shared.exception.place;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PlaceDescriptionTooShortException extends Exception {
    @Builder
    public PlaceDescriptionTooShortException(String message) {
        super(message);
    }
}
