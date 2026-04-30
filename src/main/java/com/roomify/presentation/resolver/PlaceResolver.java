package com.roomify.presentation.resolver;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;

import com.roomify.domain.api.PlaceApi;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.in.PlaceFilterInput;
import com.roomify.presentation.models.out.AvailableSlot;
import com.roomify.presentation.models.out.PlacePage;
import com.roomify.presentation.models.out.PlaceResponse;
import com.roomify.shared.exception.place.PlaceFilterInvalidException;
import com.roomify.shared.exception.place.PlaceNotFoundException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Controller
@Slf4j
public class PlaceResolver {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_AVAILABILITY_DAYS = 30;

    private final PlaceApi placeApi;

    public PlaceResolver(PlaceApi placeApi) {
        this.placeApi = placeApi;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public PlaceResponse place(@Argument Long id) throws PlaceNotFoundException {
        log.debug("GraphQL place query — id={}", id);
        return placeApi.getById(id);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public PlacePage places(@Argument PlaceFilterInput filter, @Argument PageInfoInput pagination) {
        var validatedFilter = normalizeFilter(filter);
        var validatedPagination = normalizePagination(pagination);
        return placeApi.searchPlaces(validatedFilter, validatedPagination);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public List<AvailableSlot> availableSlots(@Argument Long placeId, @Argument String month)
            throws PlaceNotFoundException {
        YearMonth yearMonth = parseYearMonthOrThrow(month);
        log.debug("GraphQL availableSlots query — placeId={}, month={}", placeId, yearMonth);
        return placeApi.getAvailableSlots(placeId, yearMonth);
    }

    @SchemaMapping(typeName = "PlaceResponse", field = "isAvailableBetween")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public Boolean isAvailableBetween(PlaceResponse place, @Argument LocalDate from, @Argument LocalDate to) {
        validatePeriod(from, to);
        log.debug("GraphQL isAvailableBetween — placeId={}, from={}, to={}", place.id(), from, to);
        return placeApi.isAvailableBetween(place.id(), from, to);
    }

    private PlaceFilterInput normalizeFilter(@Nullable PlaceFilterInput filter) {
        if (isNull(filter)) {
            log.debug("GraphQL places query — no filter provided");
            return new PlaceFilterInput();
        }

        log.debug("GraphQL places query — types={}, statuses={}, nameContains={}, ownerId={}, " +
                  "capacity=[{},{}], price=[{},{}], availability=[{},{}]",
                filter.getTypes(), filter.getStatuses(), filter.getNameContains(), filter.getOwnerId(),
                filter.getCapacityMin(), filter.getCapacityMax(),
                filter.getPricePerHourMin(), filter.getPricePerHourMax(),
                filter.getAvailableFrom(), filter.getAvailableTo());

        if (isInvalidCapacity(filter)) {
            throw PlaceFilterInvalidException.builder()
                    .message("capacityMin must be <= capacityMax")
                    .build();
        }
        if (isInvalidPrice(filter)) {
            throw PlaceFilterInvalidException.builder()
                    .message("pricePerHourMin must be <= pricePerHourMax")
                    .build();
        }
        validateAvailabilityFilter(filter);
        return filter;
    }

    private PageInfoInput normalizePagination(@Nullable PageInfoInput pagination) {
        if (isNull(pagination)) {
            log.debug("GraphQL places query — no pagination provided, using defaults");
            return new PageInfoInput(0, DEFAULT_PAGE_SIZE);
        }

        int page = Objects.requireNonNullElse(pagination.getPage(), 0);
        int pageSize = Objects.requireNonNullElse(pagination.getPageSize(), DEFAULT_PAGE_SIZE);
        pagination.setPage(page);
        pagination.setPageSize(pageSize);

        log.debug("GraphQL places query — page={}, pageSize={}", page, pageSize);

        if (page < 0) {
            throw PlaceFilterInvalidException.builder().message("page must be >= 0").build();
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw PlaceFilterInvalidException.builder()
                    .message("pageSize must be between 1 and " + MAX_PAGE_SIZE)
                    .build();
        }
        return pagination;
    }

    // ── date validation ───────────────────────────────────────────────────────

    private static void validateAvailabilityFilter(@NonNull PlaceFilterInput filter) {
        boolean hasFrom = nonNull(filter.getAvailableFrom());
        boolean hasTo = nonNull(filter.getAvailableTo());

        if (hasFrom != hasTo) {
            throw PlaceFilterInvalidException.builder()
                    .message("availableFrom and availableTo must both be provided or both omitted")
                    .build();
        }
        if (!hasFrom) return;

        validatePeriod(filter.getAvailableFrom(), filter.getAvailableTo());
    }

    private static void validatePeriod(@NonNull LocalDate from, @NonNull LocalDate to) {
        if (from.isBefore(LocalDate.now())) {
            throw PlaceFilterInvalidException.builder()
                    .message("availableFrom must not be in the past")
                    .build();
        }
        if (from.isAfter(to)) {
            throw PlaceFilterInvalidException.builder()
                    .message("availableFrom must be <= availableTo")
                    .build();
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days > MAX_AVAILABILITY_DAYS) {
            throw PlaceFilterInvalidException.builder()
                    .message("Availability window cannot exceed %d days".formatted(MAX_AVAILABILITY_DAYS))
                    .build();
        }
    }

    private static YearMonth parseYearMonthOrThrow(@NonNull String raw) {
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw PlaceFilterInvalidException.builder()
                    .message("month must be in YYYY-MM format")
                    .build();
        }
    }

    // ── predicates ────────────────────────────────────────────────────────────

    private static boolean isInvalidCapacity(@NonNull PlaceFilterInput filter) {
        return nonNull(filter.getCapacityMin()) && nonNull(filter.getCapacityMax())
               && filter.getCapacityMin() > filter.getCapacityMax();
    }

    private static boolean isInvalidPrice(@NonNull PlaceFilterInput filter) {
        return nonNull(filter.getPricePerHourMin()) && nonNull(filter.getPricePerHourMax())
               && filter.getPricePerHourMin() > filter.getPricePerHourMax();
    }
}
