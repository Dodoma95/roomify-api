package com.roomify.domain.models;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlaceSearchFilter {
    @Nullable List<PlaceTypeEnum> types;
    @Nullable List<PlaceStatusEnum> statuses;
    @Nullable String nameContains;
    @Nullable Long ownerId;
    @Nullable Integer capacityMin;
    @Nullable Integer capacityMax;
    @Nullable BigDecimal pricePerHourMin;
    @Nullable BigDecimal pricePerHourMax;
    int page;
    int pageSize;
}
