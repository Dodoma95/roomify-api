package com.roomify.unit.service.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.domain.service.booking.BookingScheduler;
import com.roomify.domain.spi.BookingSpi;
import com.roomify.domain.spi.PlaceUnavailabilitySpi;
import com.roomify.infrastucture.models.booking.Booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSchedulerTest {

    @Mock
    private BookingSpi bookingSpi;

    @Mock
    private PlaceUnavailabilitySpi placeUnavailabilitySpi;

    @InjectMocks
    private BookingScheduler bookingScheduler;

    @Test
    void completeExpiredBookings_withExpiredBookings_marksAsCompletedAndDeletesUnavailability() {
        Booking b1 = Booking.builder().id(1L).status(BookingStatusEnum.CONFIRMED)
                .startDate(LocalDate.now().minusDays(10)).endDate(LocalDate.now().minusDays(1))
                .totalPrice(BigDecimal.valueOf(100)).build();
        Booking b2 = Booking.builder().id(2L).status(BookingStatusEnum.CONFIRMED)
                .startDate(LocalDate.now().minusDays(5)).endDate(LocalDate.now().minusDays(1))
                .totalPrice(BigDecimal.valueOf(50)).build();

        when(bookingSpi.findConfirmedBookingsEndedBefore(LocalDate.now())).thenReturn(List.of(b1, b2));
        when(bookingSpi.updateBooking(any())).thenAnswer(inv -> inv.getArgument(0));

        bookingScheduler.completeExpiredBookings();

        assertThat(b1.getStatus()).isEqualTo(BookingStatusEnum.COMPLETED);
        assertThat(b2.getStatus()).isEqualTo(BookingStatusEnum.COMPLETED);
        verify(bookingSpi, times(2)).updateBooking(any());
        verify(placeUnavailabilitySpi).deleteByBookingId(1L);
        verify(placeUnavailabilitySpi).deleteByBookingId(2L);
    }

    @Test
    void completeExpiredBookings_noExpiredBookings_doesNothing() {
        when(bookingSpi.findConfirmedBookingsEndedBefore(LocalDate.now())).thenReturn(List.of());

        bookingScheduler.completeExpiredBookings();

        verify(bookingSpi, never()).updateBooking(any());
        verify(placeUnavailabilitySpi, never()).deleteByBookingId(any());
    }
}
