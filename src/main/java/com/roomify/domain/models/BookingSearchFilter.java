package com.roomify.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BookingSearchFilter {
    @Nullable List<BookingStatusEnum> statuses;
    @Nullable LocalDate startDateFrom;
    @Nullable LocalDate startDateTo;
    @Nullable LocalDate endDateFrom;
    @Nullable LocalDate endDateTo;
    @Nullable BigDecimal totalPriceMin;
    @Nullable BigDecimal totalPriceMax;
    @Nullable Instant createdAtFrom;
    @Nullable Instant createdAtTo;
    @Nullable String notesContains;
    @Nullable Long placeId;
    @Nullable String placeNameContains;
    @Nullable List<PlaceTypeEnum> placeTypes;
    @Nullable List<PlaceStatusEnum> placeStatuses;
    @Nullable Long userId;
    @Nullable String userEmailContains;
    @Nullable Long ownerId;
    int page;
    int pageSize;
}
