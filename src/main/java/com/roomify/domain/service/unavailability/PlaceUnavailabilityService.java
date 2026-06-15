package com.roomify.domain.service.unavailability;

import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.api.PlaceUnavailabilityApi;
import com.roomify.domain.models.UnavailabilityReasonEnum;
import com.roomify.domain.service.unavailability.mapper.PlaceUnavailabilityMapper;
import com.roomify.domain.spi.PlaceSpi;
import com.roomify.domain.spi.PlaceUnavailabilitySpi;
import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.place.PlaceUnavailability;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.BlockDatesRequest;
import com.roomify.presentation.models.out.PlaceUnavailabilityResponse;
import com.roomify.shared.exception.booking.BookingInvalidDatesException;
import com.roomify.shared.exception.booking.BookingUnavailableDatesException;
import com.roomify.shared.exception.booking.PlaceUnavailabilityNotFoundException;
import com.roomify.shared.exception.place.PlaceNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;

import static com.roomify.domain.service.user.UserUtils.isAdmin;
import static com.roomify.domain.service.user.UserUtils.isOwner;

@Service
public class PlaceUnavailabilityService implements PlaceUnavailabilityApi {

    private final PlaceUnavailabilitySpi placeUnavailabilitySpi;
    private final PlaceSpi placeSpi;
    private final PlaceUnavailabilityMapper placeUnavailabilityMapper;

    public PlaceUnavailabilityService(PlaceUnavailabilitySpi placeUnavailabilitySpi,
            PlaceSpi placeSpi,
            PlaceUnavailabilityMapper placeUnavailabilityMapper) {
        this.placeUnavailabilitySpi = placeUnavailabilitySpi;
        this.placeSpi = placeSpi;
        this.placeUnavailabilityMapper = placeUnavailabilityMapper;
    }

    @Transactional
    @Override
    public PlaceUnavailabilityResponse blockDates(@NonNull Long placeId, @NonNull BlockDatesRequest request,
            @NonNull User currentUser) throws PlaceNotFoundException, UserActionForbiddenException, BookingInvalidDatesException,
            BookingUnavailableDatesException {

        Place place = getPlace(placeId);
        controlOwnership(place, currentUser);
        controlDates(request.startDate(), request.endDate());
        controlNoOverlap(placeId, request.startDate(), request.endDate());

        PlaceUnavailability unavailability = placeUnavailabilitySpi.insertUnavailability(
                PlaceUnavailability.builder()
                        .place(place)
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .reason(UnavailabilityReasonEnum.OWNER_BLOCKED)
                        .build()
        );

        return placeUnavailabilityMapper.toResponse(unavailability);
    }

    @Transactional
    @Override
    public void unblockDates(@NonNull Long placeId, @NonNull Long unavailabilityId, @NonNull User currentUser)
            throws PlaceNotFoundException, PlaceUnavailabilityNotFoundException, UserActionForbiddenException {

        getPlace(placeId);

        PlaceUnavailability unavailability = placeUnavailabilitySpi.findById(unavailabilityId)
                .orElseThrow(() -> PlaceUnavailabilityNotFoundException.builder()
                        .message("Unavailability entry not found with id: %s".formatted(unavailabilityId))
                        .build());

        if (!unavailability.getPlace().getId().equals(placeId)) {
            throw PlaceUnavailabilityNotFoundException.builder()
                    .message("Unavailability entry not found with id: %s".formatted(unavailabilityId))
                    .build();
        }

        controlOwnership(unavailability.getPlace(), currentUser);

        if (!UnavailabilityReasonEnum.OWNER_BLOCKED.equals(unavailability.getReason())) {
            throw UserActionForbiddenException.builder()
                    .message("Cannot manually remove a booking-linked unavailability. Cancel the booking instead.")
                    .build();
        }

        placeUnavailabilitySpi.deleteById(unavailabilityId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PlaceUnavailabilityResponse> getPlaceUnavailability(@NonNull Long placeId)
            throws PlaceNotFoundException {
        getPlace(placeId);
        return placeUnavailabilityMapper.toResponseList(placeUnavailabilitySpi.findByPlaceId(placeId));
    }

    private Place getPlace(@NonNull Long placeId) throws PlaceNotFoundException {
        return placeSpi.findById(placeId)
                .orElseThrow(() -> PlaceNotFoundException.builder()
                        .message("Place not found with id: %s".formatted(placeId))
                        .build());
    }

    private static void controlOwnership(@NonNull Place place, @NonNull User currentUser)
            throws UserActionForbiddenException {
        if (!isOwner(currentUser, place) && !isAdmin(currentUser)) {
            throw UserActionForbiddenException.builder()
                    .message("You are not allowed to manage unavailability for this place.")
                    .build();
        }
    }

    private static void controlDates(@NonNull LocalDate startDate, @NonNull LocalDate endDate)
            throws BookingInvalidDatesException {
        if (!endDate.isAfter(startDate)) {
            throw BookingInvalidDatesException.builder()
                    .message("End date must be strictly after start date.")
                    .build();
        }
    }

    private void controlNoOverlap(@NonNull Long placeId, @NonNull LocalDate startDate, @NonNull LocalDate endDate)
            throws BookingUnavailableDatesException {
        if (placeUnavailabilitySpi.hasOverlap(placeId, startDate, endDate)) {
            throw BookingUnavailableDatesException.builder()
                    .message("These dates already overlap with an existing unavailability or booking.")
                    .build();
        }
    }
}
