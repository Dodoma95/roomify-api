package com.roomify.presentation.models.in;

import java.util.List;

import com.roomify.domain.models.PlaceStatusEnum;
import com.roomify.domain.models.PlaceTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaceFilterInput {

    private List<PlaceTypeEnum> types;
    private List<PlaceStatusEnum> statuses;
    private String nameContains;
    private Long ownerId;
    private Integer capacityMin;
    private Integer capacityMax;
    private Double pricePerHourMin;
    private Double pricePerHourMax;
}
