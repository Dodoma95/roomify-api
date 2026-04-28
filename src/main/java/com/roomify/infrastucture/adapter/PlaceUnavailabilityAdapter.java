package com.roomify.infrastucture.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.roomify.domain.spi.PlaceUnavailabilitySpi;
import com.roomify.infrastucture.models.place.PlaceUnavailability;
import com.roomify.infrastucture.repository.PlaceUnavailabilityRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PlaceUnavailabilityAdapter implements PlaceUnavailabilitySpi {

    private final PlaceUnavailabilityRepository placeUnavailabilityRepository;

    public PlaceUnavailabilityAdapter(PlaceUnavailabilityRepository placeUnavailabilityRepository) {
        this.placeUnavailabilityRepository = placeUnavailabilityRepository;
    }

    @Override
    public boolean hasOverlap(@NonNull Long placeId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
        log.debug("Checking unavailability overlap for place {} between {} and {}", placeId, startDate, endDate);
        return placeUnavailabilityRepository.hasOverlap(placeId, startDate, endDate);
    }

    @Override
    public PlaceUnavailability insertUnavailability(@NonNull PlaceUnavailability unavailability) {
        log.debug("Inserting unavailability for place {} from {} to {}", unavailability.getPlace().getId(), unavailability.getStartDate(),
                unavailability.getEndDate());
        return placeUnavailabilityRepository.save(unavailability);
    }

    @Override
    public void deleteByBookingId(@NonNull Long bookingId) {
        log.debug("Deleting unavailability entries for booking {}", bookingId);
        placeUnavailabilityRepository.deleteByBookingId(bookingId);
    }

    @Override
    public void deleteById(@NonNull Long id) {
        log.debug("Deleting unavailability entry {}", id);
        placeUnavailabilityRepository.deleteById(id);
    }

    @Override
    public Optional<PlaceUnavailability> findById(@NonNull Long id) {
        log.debug("Looking up unavailability entry {}", id);
        return placeUnavailabilityRepository.findById(id);
    }

    @Override
    public List<PlaceUnavailability> findByPlaceId(@NonNull Long placeId) {
        log.debug("Looking up unavailability entries for place {}", placeId);
        return placeUnavailabilityRepository.findByPlaceId(placeId);
    }

    @Override
    public List<PlaceUnavailability> findByPlaceIdAndOverlappingPeriod(
            @NonNull Long placeId,
            @NonNull LocalDate from,
            @NonNull LocalDate to) {
        log.debug("Looking up unavailability entries for place {} overlapping [{}, {}]", placeId, from, to);
        return placeUnavailabilityRepository.findByPlaceIdAndOverlappingPeriod(placeId, from, to);
    }
}
