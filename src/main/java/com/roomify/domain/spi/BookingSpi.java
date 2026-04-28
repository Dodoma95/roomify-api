package com.roomify.domain.spi;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.infrastucture.models.booking.Booking;

public interface BookingSpi {

    Booking insertBooking(@NonNull Booking booking);

    Booking updateBooking(@NonNull Booking booking);

    Optional<Booking> findById(@NonNull Long id);

    List<Booking> findByUserId(@NonNull Long userId, @Nullable BookingStatusEnum status);

    List<Booking> findByPlaceId(@NonNull Long placeId, @Nullable BookingStatusEnum status);

    List<Booking> findConfirmedBookingsEndedBefore(@NonNull LocalDate date);
}
