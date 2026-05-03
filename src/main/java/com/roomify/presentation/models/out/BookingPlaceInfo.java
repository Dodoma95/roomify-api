package com.roomify.presentation.models.out;

import java.math.BigDecimal;

import org.jspecify.annotations.NonNull;

public record BookingPlaceInfo(
        @NonNull Long id,
        @NonNull String name,
        @NonNull String address,
        @NonNull String type,
        @NonNull String status,
        @NonNull BigDecimal pricePerHour,
        @NonNull OwnerInfo owner
) {}
