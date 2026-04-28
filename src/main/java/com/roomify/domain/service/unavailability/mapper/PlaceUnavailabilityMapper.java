package com.roomify.domain.service.unavailability.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.roomify.infrastucture.models.place.PlaceUnavailability;
import com.roomify.presentation.models.out.PlaceUnavailabilityResponse;

@Mapper
public interface PlaceUnavailabilityMapper {

    @Mapping(source = "place.id", target = "placeId")
    PlaceUnavailabilityResponse toResponse(PlaceUnavailability unavailability);

    List<PlaceUnavailabilityResponse> toResponseList(List<PlaceUnavailability> unavailabilities);
}
