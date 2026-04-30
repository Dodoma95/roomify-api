package com.roomify.domain.service.place;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.api.PlaceApi;
import com.roomify.domain.models.PlaceSearchFilter;
import com.roomify.domain.models.PlaceStatusEnum;
import com.roomify.domain.models.RoleEnum;
import com.roomify.domain.service.place.mapper.PlaceMapper;
import com.roomify.domain.spi.PlaceSpi;
import com.roomify.domain.spi.PlaceUnavailabilitySpi;
import com.roomify.domain.spi.RoleSpi;
import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.place.PlaceUnavailability;
import com.roomify.infrastucture.models.user.Role;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.in.PlaceFilterInput;
import com.roomify.presentation.models.in.PlaceRequest;
import com.roomify.presentation.models.in.UpdatePlaceRequest;
import com.roomify.presentation.models.out.AvailableSlot;
import com.roomify.presentation.models.out.PageInfo;
import com.roomify.presentation.models.out.PlacePage;
import com.roomify.presentation.models.out.PlaceResponse;
import com.roomify.shared.exception.place.CapacityIncoherenteException;
import com.roomify.shared.exception.place.PlaceDescriptionTooShortException;
import com.roomify.shared.exception.place.PlaceDuplicationException;
import com.roomify.shared.exception.place.PlaceNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import static com.roomify.domain.service.user.UserUtils.isAdmin;
import static com.roomify.domain.service.user.UserUtils.isOwner;
import static com.roomify.shared.utils.UtilsRequest.normalizeAddress;
import static com.roomify.shared.utils.UtilsRequest.normalizeName;
import static java.util.Objects.nonNull;

@Service
public class PlaceService implements PlaceApi {

    private final PlaceSpi placeSpi;
    private final PlaceUnavailabilitySpi placeUnavailabilitySpi;
    private final RoleSpi roleSpi;
    private final PlaceMapper placeMapper;

    public PlaceService(PlaceSpi placeSpi, PlaceUnavailabilitySpi placeUnavailabilitySpi,
            RoleSpi roleSpi, PlaceMapper placeMapper) {
        this.placeSpi = placeSpi;
        this.placeUnavailabilitySpi = placeUnavailabilitySpi;
        this.roleSpi = roleSpi;
        this.placeMapper = placeMapper;
    }

    @Transactional(readOnly = true)
    @Override
    public PlaceResponse getById(@NonNull Long id) throws PlaceNotFoundException {
        return placeMapper.toResponse(getPlace(id));
    }

    @Transactional
    @Override
    @RateLimiter(name = "creationalRateLimiter")
    public PlaceResponse create(@NonNull PlaceRequest request, @NonNull User user)
            throws PlaceDuplicationException, CapacityIncoherenteException, PlaceDescriptionTooShortException {
        var normalizedName = normalizeName(request.name());
        var normalizeAddress = normalizeAddress(request.address());
        controlUniquePlaceByUser(normalizedName, normalizeAddress, user);
        controlCapacity(request.capacity());
        controlDescription(request.description());

        Place place = placeSpi.insertPlace(buildPlace(request, normalizedName, normalizeAddress, user));
        return placeMapper.toResponse(place);
    }

    @Transactional
    @Override
    @RateLimiter(name = "creationalRateLimiter")
    @SuppressWarnings("java:S2637") // suppression du warning de nullabilité sur les champs de UpdatePlaceRequest, faux positif
    public PlaceResponse update(@NonNull Long id, @NonNull UpdatePlaceRequest request, @NonNull User currentUser)
            throws PlaceNotFoundException, UserActionForbiddenException, PlaceDuplicationException,
            CapacityIncoherenteException, PlaceDescriptionTooShortException {
        Place place = getPlace(id);
        controlOwnership(place, currentUser);

        boolean nameChanged = nonNull(request.name());
        boolean addressChanged = nonNull(request.address());

        if (nameChanged || addressChanged) {
            String nameToCheck = nameChanged ? normalizeName(request.name()) : place.getName();
            String addressToCheck = addressChanged ? normalizeAddress(request.address()) : place.getNormalizedAddress();
            controlUniquePlaceByUserExcluding(nameToCheck, addressToCheck, currentUser, id);
            if (nameChanged) {
                place.setName(normalizeName(request.name()));
            }
            if (addressChanged) {
                place.setAddress(request.address());
                place.setNormalizedAddress(normalizeAddress(request.address()));
            }
        }

        if (nonNull(request.description())) {
            controlDescription(request.description());
            place.setDescription(request.description());
        }
        if (nonNull(request.type())) {
            place.setType(request.type());
        }
        if (nonNull(request.capacity())) {
            controlCapacity(request.capacity());
            place.setCapacity(request.capacity());
        }
        if (nonNull(request.pricePerHour())) {
            place.setPricePerHour(request.pricePerHour());
        }

        if (PlaceStatusEnum.APPROVED.equals(place.getStatus())) {
            place.setStatus(PlaceStatusEnum.PENDING);
        }

        return placeMapper.toResponse(placeSpi.updatePlace(place));
    }

    @Transactional
    @Override
    public void delete(@NonNull Long id, @NonNull User currentUser)
            throws PlaceNotFoundException, UserActionForbiddenException {
        Place place = getPlace(id);
        controlOwnership(place, currentUser);
        placeSpi.deletePlace(id);
    }

    @Transactional(readOnly = true)
    @Override
    public PlacePage searchPlaces(@NonNull PlaceFilterInput filter, @NonNull PageInfoInput pagination) {
        PlaceSearchFilter searchFilter = PlaceSearchFilter.builder()
                .types(filter.getTypes())
                .statuses(filter.getStatuses())
                .nameContains(filter.getNameContains())
                .ownerId(filter.getOwnerId())
                .capacityMin(filter.getCapacityMin())
                .capacityMax(filter.getCapacityMax())
                .pricePerHourMin(nonNull(filter.getPricePerHourMin())
                        ? BigDecimal.valueOf(filter.getPricePerHourMin()) : null)
                .pricePerHourMax(nonNull(filter.getPricePerHourMax())
                        ? BigDecimal.valueOf(filter.getPricePerHourMax()) : null)
                .availableFrom(filter.getAvailableFrom())
                .availableTo(filter.getAvailableTo())
                .page(pagination.getPage())
                .pageSize(pagination.getPageSize())
                .build();

        Page<Place> page = placeSpi.searchPlaces(searchFilter);
        PageInfo pageInfo = new PageInfo(
                searchFilter.getPage(),
                searchFilter.getPageSize(),
                (int) page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
        return new PlacePage(placeMapper.toResponseList(page.getContent()), pageInfo);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isAvailableBetween(@NonNull Long placeId, @NonNull LocalDate from, @NonNull LocalDate to) {
        return !placeUnavailabilitySpi.hasOverlap(placeId, from, to);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AvailableSlot> getAvailableSlots(@NonNull Long placeId, @NonNull YearMonth month) throws PlaceNotFoundException {
        getPlace(placeId);

        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();

        List<PlaceUnavailability> blocked = placeUnavailabilitySpi.findByPlaceIdAndOverlappingPeriod(placeId, first, last);

        List<AvailableSlot> slots = new ArrayList<>();
        LocalDate cursor = first;

        for (PlaceUnavailability u : mergeOverlapping(blocked, first, last)) {
            if (cursor.isBefore(u.getStartDate())) {
                slots.add(new AvailableSlot(cursor, u.getStartDate().minusDays(1)));
            }
            if (u.getEndDate().isAfter(cursor)) {
                cursor = u.getEndDate().plusDays(1);
            }
        }

        if (!cursor.isAfter(last)) {
            slots.add(new AvailableSlot(cursor, last));
        }

        return slots;
    }

    /**
     * Fusionne les périodes d'indisponibilité chevauchantes ou adjacentes,
     * bornées par [rangeStart, rangeEnd].
     */
    private static List<PlaceUnavailability> mergeOverlapping(
            List<PlaceUnavailability> sorted, LocalDate rangeStart, LocalDate rangeEnd) {
        List<PlaceUnavailability> merged = new ArrayList<>();
        for (PlaceUnavailability u : sorted) {
            LocalDate start = u.getStartDate().isBefore(rangeStart) ? rangeStart : u.getStartDate();
            LocalDate end = u.getEndDate().isAfter(rangeEnd) ? rangeEnd : u.getEndDate();

            if (merged.isEmpty() || start.isAfter(merged.getLast().getEndDate().plusDays(1))) {
                merged.add(synthetic(start, end));
            } else if (end.isAfter(merged.getLast().getEndDate())) {
                merged.set(merged.size() - 1, synthetic(merged.getLast().getStartDate(), end));
            }
        }
        return merged;
    }

    private static PlaceUnavailability synthetic(LocalDate start, LocalDate end) {
        return PlaceUnavailability.builder().startDate(start).endDate(end).build();
    }

    private Place getPlace(@NonNull Long id) throws PlaceNotFoundException {
        return placeSpi.findById(id)
                .orElseThrow(() -> PlaceNotFoundException.builder()
                        .message("Place not found with id: %s".formatted(id))
                        .build());
    }

    private void controlUniquePlaceByUser(
            @NonNull String name,
            @NonNull String adress,
            @NonNull User user) throws PlaceDuplicationException {
        if (placeSpi.existsPlaceByUser(user, name, adress)) {
            throw PlaceDuplicationException.builder()
                    .message("Place with the same name and address already exists for this user.")
                    .build();
        }
    }

    private void controlUniquePlaceByUserExcluding(
            @NonNull String name,
            @NonNull String address,
            @NonNull User user,
            @NonNull Long excludeId) throws PlaceDuplicationException {
        if (placeSpi.existsPlaceByUserExcluding(user, name, address, excludeId)) {
            throw PlaceDuplicationException.builder()
                    .message("Place with the same name and address already exists for this user.")
                    .build();
        }
    }

    private Place buildPlace(
            @NonNull PlaceRequest request,
            @NonNull String normalizedName,
            @NonNull String normalizedAddress,
            @NonNull User currentUser) {
        Place.PlaceBuilder builder = Place.builder();
        builder.name(normalizedName);
        builder.normalizedAddress(normalizedAddress);
        builder.address(request.address());
        builder.type(request.type());
        builder.status(PlaceStatusEnum.PENDING);
        Optional.ofNullable(request.description()).ifPresent(builder::description);
        Optional.ofNullable(request.capacity()).ifPresent(builder::capacity);
        builder.pricePerHour(request.pricePerHour());

        if (!currentUser.getRolesEnum().contains(RoleEnum.OWNER)) {
            Optional<Role> byName = roleSpi.findByName(RoleEnum.OWNER);
            byName.ifPresent(currentUser.getRoles()::add);
        }
        builder.owner(currentUser);
        return builder.build();
    }

    private static void controlDescription(@Nullable String description) throws PlaceDescriptionTooShortException {
        if (nonNull(description) && description.length() < 20) {
            throw PlaceDescriptionTooShortException.builder()
                    .message("Description too short")
                    .build();
        }
    }

    private static void controlCapacity(@Nullable Integer capacity) throws CapacityIncoherenteException {
        if (nonNull(capacity) && capacity <= 0) {
            throw CapacityIncoherenteException.builder()
                    .message("Capacity must be greater than 0")
                    .build();
        }
    }

    private static void controlOwnership(@NonNull Place place, @NonNull User currentUser) throws UserActionForbiddenException {
        if (!isOwner(currentUser, place) && !isAdmin(currentUser)) {
            throw UserActionForbiddenException.builder()
                    .message("You are not allowed to modify this place.")
                    .build();
        }
    }
}
