package com.roomify.presentation.resolver;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.roomify.domain.api.BookingApi;
import com.roomify.presentation.models.in.BookingFilterInput;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.out.BookingPage;
import com.roomify.shared.exception.booking.BookingFilterInvalidException;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Controller
@Slf4j
public class BookingResolver {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final BookingApi bookingApi;

    public BookingResolver(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public BookingPage bookings(@Argument BookingFilterInput filter, @Argument PageInfoInput pagination) {
        var validatedPagination = normalizePagination(pagination);
        validateFilter(filter);
        log.debug("GraphQL bookings query — filter={}, page={}, pageSize={}",
                filter, validatedPagination.getPage(), validatedPagination.getPageSize());
        return bookingApi.searchBookings(filter, validatedPagination);
    }

    private PageInfoInput normalizePagination(@Nullable PageInfoInput pagination) {
        if (isNull(pagination)) {
            return new PageInfoInput(0, DEFAULT_PAGE_SIZE);
        }

        int page = Objects.requireNonNullElse(pagination.getPage(), 0);
        int pageSize = Objects.requireNonNullElse(pagination.getPageSize(), DEFAULT_PAGE_SIZE);
        pagination.setPage(page);
        pagination.setPageSize(pageSize);

        if (page < 0) {
            throw BookingFilterInvalidException.builder().message("page must be >= 0").build();
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw BookingFilterInvalidException.builder()
                    .message("pageSize must be between 1 and " + MAX_PAGE_SIZE)
                    .build();
        }
        return pagination;
    }

    private static void validateFilter(@Nullable BookingFilterInput filter) {
        if (isNull(filter)) return;

        validateDateRange("startDateFrom", filter.getStartDateFrom(), "startDateTo", filter.getStartDateTo());
        validateDateRange("endDateFrom", filter.getEndDateFrom(), "endDateTo", filter.getEndDateTo());
        validatePriceRange(filter.getTotalPriceMin(), filter.getTotalPriceMax());
        validateInstantRange(filter.getCreatedAtFrom(), filter.getCreatedAtTo());
    }

    private static void validateDateRange(String fromName, @Nullable LocalDate from,
                                          String toName, @Nullable LocalDate to) {
        if (nonNull(from) && nonNull(to) && from.isAfter(to)) {
            throw BookingFilterInvalidException.builder()
                    .message("%s must be <= %s".formatted(fromName, toName))
                    .build();
        }
    }

    private static void validatePriceRange(@Nullable Double min, @Nullable Double max) {
        if (nonNull(min) && min < 0) {
            throw BookingFilterInvalidException.builder()
                    .message("totalPriceMin must be >= 0")
                    .build();
        }
        if (nonNull(min) && nonNull(max) && BigDecimal.valueOf(min).compareTo(BigDecimal.valueOf(max)) > 0) {
            throw BookingFilterInvalidException.builder()
                    .message("totalPriceMin must be <= totalPriceMax")
                    .build();
        }
    }

    private static void validateInstantRange(@Nullable Instant from, @Nullable Instant to) {
        if (nonNull(from) && nonNull(to) && from.isAfter(to)) {
            throw BookingFilterInvalidException.builder()
                    .message("createdAtFrom must be <= createdAtTo")
                    .build();
        }
    }
}
