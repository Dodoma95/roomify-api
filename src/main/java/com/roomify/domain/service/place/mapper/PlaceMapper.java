package com.roomify.domain.service.place.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.out.OwnerInfo;
import com.roomify.presentation.models.out.PlaceResponse;

@Mapper
public interface PlaceMapper {

    PlaceResponse toResponse(Place place);

    List<PlaceResponse> toResponseList(List<Place> places);

    OwnerInfo toOwnerInfo(User owner);

}
