package com.roomify.domain.service.booking;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.domain.spi.BookingSpi;
import com.roomify.domain.spi.PlaceUnavailabilitySpi;
import com.roomify.infrastucture.models.booking.Booking;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BookingScheduler {

    private final BookingSpi bookingSpi;
    private final PlaceUnavailabilitySpi placeUnavailabilitySpi;

    public BookingScheduler(BookingSpi bookingSpi, PlaceUnavailabilitySpi placeUnavailabilitySpi) {
        this.bookingSpi = bookingSpi;
        this.placeUnavailabilitySpi = placeUnavailabilitySpi;
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * ?")
    public void completeExpiredBookings() {
        LocalDate today = LocalDate.now();
        List<Booking> expired = bookingSpi.findConfirmedBookingsEndedBefore(today);
        if (expired.isEmpty()) {
            return;
        }
        log.info("Completing {} expired booking(s)", expired.size());
        for (Booking booking : expired) {
            booking.setStatus(BookingStatusEnum.COMPLETED);
            bookingSpi.updateBooking(booking);
            placeUnavailabilitySpi.deleteByBookingId(booking.getId());
        }
    }
}
