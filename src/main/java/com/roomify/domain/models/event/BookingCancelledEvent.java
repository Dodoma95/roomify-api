package com.roomify.domain.models.event;

import java.time.LocalDate;

public record BookingCancelledEvent(
        String tenantEmail,
        String tenantFirstName,
        String placeName,
        LocalDate startDate,
        LocalDate endDate
) {}
