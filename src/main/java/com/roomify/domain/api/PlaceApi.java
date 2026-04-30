package com.roomify.domain.api;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.jspecify.annotations.NonNull;

import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.in.PlaceFilterInput;
import com.roomify.presentation.models.in.PlaceRequest;
import com.roomify.presentation.models.in.UpdatePlaceRequest;
import com.roomify.presentation.models.out.AvailableSlot;
import com.roomify.presentation.models.out.PlacePage;
import com.roomify.presentation.models.out.PlaceResponse;
import com.roomify.shared.exception.place.CapacityIncoherenteException;
import com.roomify.shared.exception.place.PlaceDescriptionTooShortException;
import com.roomify.shared.exception.place.PlaceDuplicationException;
import com.roomify.shared.exception.place.PlaceNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;

public interface PlaceApi {

    PlaceResponse getById(@NonNull Long id) throws PlaceNotFoundException;

    PlaceResponse create(@NonNull PlaceRequest request, @NonNull User user)
            throws PlaceDuplicationException, CapacityIncoherenteException, PlaceDescriptionTooShortException;

    PlaceResponse update(@NonNull Long id, @NonNull UpdatePlaceRequest request, @NonNull User currentUser)
            throws PlaceNotFoundException, UserActionForbiddenException, PlaceDuplicationException,
                   CapacityIncoherenteException, PlaceDescriptionTooShortException;

    void delete(@NonNull Long id, @NonNull User currentUser)
            throws PlaceNotFoundException, UserActionForbiddenException;

    PlacePage searchPlaces(@NonNull PlaceFilterInput filter, @NonNull PageInfoInput pagination);

    boolean isAvailableBetween(@NonNull Long placeId, @NonNull LocalDate from, @NonNull LocalDate to);

    List<AvailableSlot> getAvailableSlots(@NonNull Long placeId, @NonNull YearMonth month) throws PlaceNotFoundException;
}
