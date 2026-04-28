package com.roomify.domain.spi;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

import com.roomify.infrastucture.models.place.PlaceUnavailability;

public interface PlaceUnavailabilitySpi {

    boolean hasOverlap(@NonNull Long placeId, @NonNull LocalDate startDate, @NonNull LocalDate endDate);

    PlaceUnavailability insertUnavailability(@NonNull PlaceUnavailability unavailability);

    void deleteByBookingId(@NonNull Long bookingId);

    void deleteById(@NonNull Long id);

    Optional<PlaceUnavailability> findById(@NonNull Long id);

    List<PlaceUnavailability> findByPlaceId(@NonNull Long placeId);

    List<PlaceUnavailability> findByPlaceIdAndOverlappingPeriod(@NonNull Long placeId, @NonNull LocalDate from, @NonNull LocalDate to);
}
