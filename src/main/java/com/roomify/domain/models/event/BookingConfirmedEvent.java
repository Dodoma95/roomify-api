package com.roomify.domain.models.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingConfirmedEvent(
        String tenantEmail,
        String tenantFirstName,
        String placeName,
        String placeAddress,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalPrice
) {}
