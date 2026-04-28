package com.roomify.domain.models.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingRequestedEvent(
        Long bookingId,
        String tenantEmail,
        String tenantFirstName,
        String ownerEmail,
        String ownerFirstName,
        String placeName,
        String placeAddress,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalPrice
) {}
