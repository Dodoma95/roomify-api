package com.roomify.infrastucture.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.domain.spi.BookingSpi;
import com.roomify.infrastucture.models.booking.Booking;
import com.roomify.infrastucture.repository.BookingRepository;

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
}
