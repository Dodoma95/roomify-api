package com.roomify.domain.service.booking.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.roomify.infrastucture.models.booking.Booking;
import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.out.BookingGraphQLResponse;
import com.roomify.presentation.models.out.BookingPlaceInfo;
import com.roomify.presentation.models.out.BookingResponse;
import com.roomify.presentation.models.out.OwnerInfo;

@Mapper
public interface BookingMapper {

    @Mapping(source = "place.id", target = "placeId")
    @Mapping(source = "place.name", target = "placeName")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.firstName", target = "userFirstName")
    @Mapping(source = "user.lastName", target = "userLastName")
    BookingResponse toResponse(Booking booking);

    List<BookingResponse> toResponseList(List<Booking> bookings);

    @Mapping(source = "user", target = "booker")
    @Mapping(target = "createdAt", expression = "java(booking.getCreatedAt().toInstant(java.time.ZoneOffset.UTC))")
    BookingGraphQLResponse toGraphQLResponse(Booking booking);

    List<BookingGraphQLResponse> toGraphQLResponseList(List<Booking> bookings);

    BookingPlaceInfo toBookingPlaceInfo(Place place);

    OwnerInfo toOwnerInfo(User user);
}
