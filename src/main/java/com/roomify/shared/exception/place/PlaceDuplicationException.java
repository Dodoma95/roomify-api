package com.roomify.shared.exception.place;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PlaceDuplicationException extends Exception {
    @Builder
    public PlaceDuplicationException(String message) {
        super(message);
    }
}
