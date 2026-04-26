package com.roomify.presentation.resolver;

import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;

import com.roomify.domain.api.PlaceApi;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.in.PlaceFilterInput;
import com.roomify.presentation.models.out.PlacePage;
import com.roomify.shared.exception.place.PlaceFilterInvalidException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Controller
@Slf4j
public class PlaceResolver {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final PlaceApi placeApi;

    public PlaceResolver(PlaceApi placeApi) {
        this.placeApi = placeApi;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public PlacePage places(@Argument PlaceFilterInput filter, @Argument PageInfoInput pagination) {
        var validatedFilter = normalizeFilter(filter);
        var validatedPagination = normalizePagination(pagination);
        return placeApi.searchPlaces(validatedFilter, validatedPagination);
    }

    private PlaceFilterInput normalizeFilter(@Nullable PlaceFilterInput filter) {
        if (isNull(filter)) {
            log.debug("GraphQL places query — no filter provided");
            return new PlaceFilterInput();
        }

        log.debug("GraphQL places query — types={}, statuses={}, nameContains={}, ownerId={}, capacity=[{},{}], price=[{},{}]",
                filter.getTypes(), filter.getStatuses(), filter.getNameContains(), filter.getOwnerId(),
                filter.getCapacityMin(), filter.getCapacityMax(),
                filter.getPricePerHourMin(), filter.getPricePerHourMax());

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

    private static boolean isInvalidCapacity(@NonNull PlaceFilterInput filter) {
        return nonNull(filter.getCapacityMin()) && nonNull(filter.getCapacityMax())
               && filter.getCapacityMin() > filter.getCapacityMax();
    }

    private static boolean isInvalidPrice(@NonNull PlaceFilterInput filter) {
        return nonNull(filter.getPricePerHourMin()) && nonNull(filter.getPricePerHourMax())
               && filter.getPricePerHourMin() > filter.getPricePerHourMax();
    }
}
