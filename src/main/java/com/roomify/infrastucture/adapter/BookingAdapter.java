package com.roomify.infrastucture.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.roomify.domain.models.BookingSearchFilter;
import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.domain.spi.BookingSpi;
import com.roomify.infrastucture.models.booking.Booking;
import com.roomify.infrastucture.repository.BookingRepository;
import com.roomify.infrastucture.repository.specification.BookingSpecifications;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.nonNull;

@Component
@Slf4j
public class BookingAdapter implements BookingSpi {

    private final BookingRepository bookingRepository;

    public BookingAdapter(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Booking insertBooking(@NonNull Booking booking) {
        log.debug("Inserting booking for place {} by user {}", booking.getPlace().getId(), booking.getUser().getId());
        return bookingRepository.save(booking);
    }

    @Override
    public Booking updateBooking(@NonNull Booking booking) {
        log.debug("Updating booking {}", booking.getId());
        return bookingRepository.save(booking);
    }

    @Override
    public Optional<Booking> findById(@NonNull Long id) {
        log.debug("Looking up booking by id {}", id);
        return bookingRepository.findById(id);
    }

    @Override
    public List<Booking> findByUserId(@NonNull Long userId, @Nullable BookingStatusEnum status) {
        log.debug("Looking up bookings for user {}, status={}", userId, status);
        if (nonNull(status)) {
            return bookingRepository.findByUserIdAndStatus(userId, status);
        }
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> findByPlaceId(@NonNull Long placeId, @Nullable BookingStatusEnum status) {
        log.debug("Looking up bookings for place {}, status={}", placeId, status);
        if (nonNull(status)) {
            return bookingRepository.findByPlaceIdAndStatus(placeId, status);
        }
        return bookingRepository.findByPlaceId(placeId);
    }

    @Override
    public List<Booking> findConfirmedBookingsEndedBefore(@NonNull LocalDate date) {
        log.debug("Looking up confirmed bookings ended before {}", date);
        return bookingRepository.findByStatusAndEndDateBefore(BookingStatusEnum.CONFIRMED, date);
    }

    @Override
    @SuppressWarnings("java:S3776") // Suppress "Cognitive Complexity" warning for this method
    public @NonNull Page<Booking> searchBookings(@NonNull BookingSearchFilter filter) {
        log.debug("Searching bookings page={}, size={}", filter.getPage(), filter.getPageSize());

        Specification<Booking> spec = (root, query, cb) -> cb.conjunction();

        if (nonNull(filter.getStatuses()) && !filter.getStatuses().isEmpty()) {
            spec = spec.and(BookingSpecifications.hasStatuses(filter.getStatuses()));
        }
        if (nonNull(filter.getStartDateFrom())) {
            spec = spec.and(BookingSpecifications.startDateFrom(filter.getStartDateFrom()));
        }
        if (nonNull(filter.getStartDateTo())) {
            spec = spec.and(BookingSpecifications.startDateTo(filter.getStartDateTo()));
        }
        if (nonNull(filter.getEndDateFrom())) {
            spec = spec.and(BookingSpecifications.endDateFrom(filter.getEndDateFrom()));
        }
        if (nonNull(filter.getEndDateTo())) {
            spec = spec.and(BookingSpecifications.endDateTo(filter.getEndDateTo()));
        }
        if (nonNull(filter.getTotalPriceMin())) {
            spec = spec.and(BookingSpecifications.totalPriceMin(filter.getTotalPriceMin()));
        }
        if (nonNull(filter.getTotalPriceMax())) {
            spec = spec.and(BookingSpecifications.totalPriceMax(filter.getTotalPriceMax()));
        }
        if (nonNull(filter.getCreatedAtFrom())) {
            spec = spec.and(BookingSpecifications.createdAtFrom(
                    LocalDateTime.ofInstant(filter.getCreatedAtFrom(), ZoneOffset.UTC)));
        }
        if (nonNull(filter.getCreatedAtTo())) {
            spec = spec.and(BookingSpecifications.createdAtTo(
                    LocalDateTime.ofInstant(filter.getCreatedAtTo(), ZoneOffset.UTC)));
        }
        if (nonNull(filter.getNotesContains()) && !filter.getNotesContains().isBlank()) {
            spec = spec.and(BookingSpecifications.notesContains(filter.getNotesContains()));
        }
        if (nonNull(filter.getPlaceId())) {
            spec = spec.and(BookingSpecifications.hasPlaceId(filter.getPlaceId()));
        }
        if (nonNull(filter.getPlaceNameContains()) && !filter.getPlaceNameContains().isBlank()) {
            spec = spec.and(BookingSpecifications.placeNameContains(filter.getPlaceNameContains()));
        }
        if (nonNull(filter.getPlaceTypes()) && !filter.getPlaceTypes().isEmpty()) {
            spec = spec.and(BookingSpecifications.hasPlaceTypes(filter.getPlaceTypes()));
        }
        if (nonNull(filter.getPlaceStatuses()) && !filter.getPlaceStatuses().isEmpty()) {
            spec = spec.and(BookingSpecifications.hasPlaceStatuses(filter.getPlaceStatuses()));
        }
        if (nonNull(filter.getUserId())) {
            spec = spec.and(BookingSpecifications.hasUserId(filter.getUserId()));
        }
        if (nonNull(filter.getUserEmailContains()) && !filter.getUserEmailContains().isBlank()) {
            spec = spec.and(BookingSpecifications.userEmailContains(filter.getUserEmailContains()));
        }
        if (nonNull(filter.getOwnerId())) {
            spec = spec.and(BookingSpecifications.hasOwnerId(filter.getOwnerId()));
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getPageSize());
        return bookingRepository.findAll(spec, pageable);
    }
}
