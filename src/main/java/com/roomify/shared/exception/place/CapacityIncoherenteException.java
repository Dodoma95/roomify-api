package com.roomify.shared.exception.place;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CapacityIncoherenteException extends Exception {
    @Builder
    public CapacityIncoherenteException(String message) {
        super(message);
    }
}
