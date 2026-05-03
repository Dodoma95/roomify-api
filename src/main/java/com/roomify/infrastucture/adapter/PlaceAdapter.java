package com.roomify.infrastucture.adapter;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.roomify.domain.models.PlaceSearchFilter;
import com.roomify.domain.spi.PlaceSpi;
import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.user.User;
import com.roomify.infrastucture.repository.PlaceRepository;
import com.roomify.infrastucture.repository.specification.PlaceSpecifications;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.nonNull;

@Component
@Slf4j
public class PlaceAdapter implements PlaceSpi {

    private final PlaceRepository placeRepository;

    public PlaceAdapter(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Override
    public boolean existsPlaceByUser(@NonNull User user, @NonNull String name, @NonNull String address) {
        log.debug("Checking if place '{}' at '{}' already exists for user {}", name, address, user.getId());
        return placeRepository.existsByOwnerAndNameIgnoreCaseAndNormalizedAddressIgnoreCase(user, name, address);
    }

    @Override
    public boolean existsPlaceByUserExcluding(@NonNull User user, @NonNull String name, @NonNull String address, @NonNull Long excludeId) {
        log.debug("Checking uniqueness of place '{}' at '{}' for user {} excluding id {}", name, address, user.getId(), excludeId);
        return placeRepository.existsByOwnerAndNameIgnoreCaseAndNormalizedAddressIgnoreCaseAndIdNot(user, name, address, excludeId);
    }

    @Override
    public Optional<Place> findById(@NonNull Long id) {
        log.debug("Looking up place by id {}", id);
        return placeRepository.findById(id);
    }

    @Override
    public Place insertPlace(@NonNull Place place) {
        log.debug("Inserting place with name {}", place.getName());
        return placeRepository.save(place);
    }

    @Override
    public Place updatePlace(@NonNull Place place) {
        log.debug("Updating place with id {}", place.getId());
        return placeRepository.save(place);
    }

    @Override
    public void deletePlace(@NonNull Long id) {
        log.debug("Deleting place with id {}", id);
        placeRepository.deleteById(id);
    }

    @Override
    public Page<Place> searchPlaces(@NonNull PlaceSearchFilter filter) {
        log.debug("Searching places page={}, size={}", filter.getPage(), filter.getPageSize());

        Specification<Place> spec = (root, query, cb) -> cb.conjunction();

        if (nonNull(filter.getTypes()) && !filter.getTypes().isEmpty()) {
            spec = spec.and(PlaceSpecifications.hasTypes(filter.getTypes()));
        }
        if (nonNull(filter.getStatuses()) && !filter.getStatuses().isEmpty()) {
            spec = spec.and(PlaceSpecifications.hasStatuses(filter.getStatuses()));
        }
        if (nonNull(filter.getNameContains()) && !filter.getNameContains().isBlank()) {
            spec = spec.and(PlaceSpecifications.nameContains(filter.getNameContains()));
        }
        if (nonNull(filter.getOwnerId())) {
            spec = spec.and(PlaceSpecifications.hasOwner(filter.getOwnerId()));
        }
        if (nonNull(filter.getCapacityMin())) {
            spec = spec.and(PlaceSpecifications.capacityAtLeast(filter.getCapacityMin()));
        }
        if (nonNull(filter.getCapacityMax())) {
            spec = spec.and(PlaceSpecifications.capacityAtMost(filter.getCapacityMax()));
        }
        if (nonNull(filter.getPricePerHourMin())) {
            spec = spec.and(PlaceSpecifications.priceAtLeast(filter.getPricePerHourMin()));
        }
        if (nonNull(filter.getPricePerHourMax())) {
            spec = spec.and(PlaceSpecifications.priceAtMost(filter.getPricePerHourMax()));
        }
        if (nonNull(filter.getAvailableFrom()) && nonNull(filter.getAvailableTo())) {
            spec = spec.and(PlaceSpecifications.isAvailableBetween(filter.getAvailableFrom(), filter.getAvailableTo()));
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getPageSize());
        return placeRepository.findAll(spec, pageable);
    }
}
