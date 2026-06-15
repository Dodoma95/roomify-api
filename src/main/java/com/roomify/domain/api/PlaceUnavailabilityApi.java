package com.roomify.domain.api;

import java.util.List;

import org.jspecify.annotations.NonNull;

import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.BlockDatesRequest;
import com.roomify.presentation.models.out.PlaceUnavailabilityResponse;
import com.roomify.shared.exception.booking.BookingInvalidDatesException;
import com.roomify.shared.exception.booking.BookingUnavailableDatesException;
import com.roomify.shared.exception.booking.PlaceUnavailabilityNotFoundException;
import com.roomify.shared.exception.place.PlaceNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;

public interface PlaceUnavailabilityApi {

    PlaceUnavailabilityResponse blockDates(@NonNull Long placeId, @NonNull BlockDatesRequest request, @NonNull User currentUser)
            throws PlaceNotFoundException, UserActionForbiddenException, BookingInvalidDatesException,
            BookingUnavailableDatesException;

    void unblockDates(@NonNull Long placeId, @NonNull Long unavailabilityId, @NonNull User currentUser)
            throws PlaceNotFoundException, PlaceUnavailabilityNotFoundException, UserActionForbiddenException;

    List<PlaceUnavailabilityResponse> getPlaceUnavailability(@NonNull Long placeId)
            throws PlaceNotFoundException;
}
