package com.roomify.domain.service.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roomify.domain.api.BookingApi;
import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.domain.models.PlaceStatusEnum;
import com.roomify.domain.models.UnavailabilityReasonEnum;
import com.roomify.domain.models.event.BookingCancelledEvent;
import com.roomify.domain.models.event.BookingConfirmedEvent;
import com.roomify.domain.models.event.BookingRequestedEvent;
import com.roomify.domain.service.booking.mapper.BookingMapper;
import com.roomify.domain.spi.BookingSpi;
import com.roomify.domain.spi.PlaceUnavailabilitySpi;
import com.roomify.domain.spi.PlaceSpi;
import com.roomify.infrastucture.models.booking.Booking;
import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.place.PlaceUnavailability;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.BookingRequest;
import com.roomify.presentation.models.out.BookingResponse;
import com.roomify.shared.exception.booking.BookingAlreadyConfirmedException;
import com.roomify.shared.exception.booking.BookingInvalidDatesException;
import com.roomify.shared.exception.booking.BookingNotCancellableException;
import com.roomify.shared.exception.booking.BookingNotFoundException;
import com.roomify.shared.exception.booking.BookingOwnPlaceException;
import com.roomify.shared.exception.booking.BookingPlaceNotApprovedException;
import com.roomify.shared.exception.booking.BookingUnavailableDatesException;
import com.roomify.shared.exception.place.PlaceNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import static com.roomify.domain.service.user.UserUtils.isAdmin;
import static com.roomify.domain.service.user.UserUtils.isOwner;

@Service
public class BookingService implements BookingApi {

    private static final int MAX_BOOKING_DAYS = 30;

    private final BookingSpi bookingSpi;
    private final PlaceUnavailabilitySpi placeUnavailabilitySpi;
    private final PlaceSpi placeSpi;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingSpi bookingSpi, PlaceUnavailabilitySpi placeUnavailabilitySpi,
                          PlaceSpi placeSpi, BookingMapper bookingMapper,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingSpi = bookingSpi;
        this.placeUnavailabilitySpi = placeUnavailabilitySpi;
        this.placeSpi = placeSpi;
        this.bookingMapper = bookingMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    @RateLimiter(name = "creationalRateLimiter")
    public BookingResponse createBooking(@NonNull BookingRequest request, @NonNull User currentUser)
            throws PlaceNotFoundException, BookingPlaceNotApprovedException, BookingOwnPlaceException,
            BookingInvalidDatesException, BookingUnavailableDatesException {

        Place place = getPlace(request.placeId());

        controlPlaceApproved(place);
        controlNotOwnPlace(place, currentUser);
        controlDates(request.startDate(), request.endDate());
        controlAvailability(place.getId(), request.startDate(), request.endDate());

        Booking booking = bookingSpi.insertBooking(buildBooking(request, place, currentUser));

        placeUnavailabilitySpi.insertUnavailability(PlaceUnavailability.builder()
                .place(place)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(UnavailabilityReasonEnum.BOOKING)
                .booking(booking)
                .build());

        User owner = place.getOwner();
        eventPublisher.publishEvent(new BookingRequestedEvent(
                booking.getId(),
                currentUser.getEmail(),
                currentUser.getFirstName(),
                owner.getEmail(),
                owner.getFirstName(),
                place.getName(),
                place.getAddress(),
                request.startDate(),
                request.endDate(),
                booking.getTotalPrice()
        ));

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    @Override
    public BookingResponse confirmBooking(@NonNull Long bookingId, @NonNull User currentUser)
            throws BookingNotFoundException, BookingAlreadyConfirmedException, UserActionForbiddenException {

        Booking booking = bookingSpi.findById(bookingId)
                .orElseThrow(() -> BookingNotFoundException.builder()
                        .message("Booking not found with id: %s".formatted(bookingId))
                        .build());

        controlConfirmPermission(booking, currentUser);
        controlBookingConfirmable(booking);

        booking.setStatus(BookingStatusEnum.CONFIRMED);
        Booking confirmed = bookingSpi.updateBooking(booking);

        User tenant = booking.getUser();
        Place place = booking.getPlace();
        eventPublisher.publishEvent(new BookingConfirmedEvent(
                tenant.getEmail(),
                tenant.getFirstName(),
                place.getName(),
                place.getAddress(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getTotalPrice()
        ));

        return bookingMapper.toResponse(confirmed);
    }

    @Transactional
    @Override
    public void cancelBooking(@NonNull Long bookingId, @NonNull User currentUser)
            throws BookingNotFoundException, BookingNotCancellableException, UserActionForbiddenException {

        Booking booking = bookingSpi.findById(bookingId)
                .orElseThrow(() -> BookingNotFoundException.builder()
                        .message("Booking not found with id: %s".formatted(bookingId))
                        .build());

        controlCancellationPermission(booking, currentUser);
        controlCancellable(booking);

        booking.setStatus(BookingStatusEnum.CANCELLED);
        bookingSpi.updateBooking(booking);
        placeUnavailabilitySpi.deleteByBookingId(bookingId);

        User tenant = booking.getUser();
        eventPublisher.publishEvent(new BookingCancelledEvent(
                tenant.getEmail(),
                tenant.getFirstName(),
                booking.getPlace().getName(),
                booking.getStartDate(),
                booking.getEndDate()
        ));
    }

    @Transactional(readOnly = true)
    @Override
    public List<BookingResponse> getMyBookings(@NonNull User currentUser, @Nullable BookingStatusEnum status) {
        return bookingMapper.toResponseList(bookingSpi.findByUserId(currentUser.getId(), status));
    }

    @Transactional(readOnly = true)
    @Override
    public List<BookingResponse> getBookingsByPlace(@NonNull Long placeId, @NonNull User currentUser)
            throws PlaceNotFoundException, UserActionForbiddenException {

        Place place = getPlace(placeId);
        controlPlaceAccess(place, currentUser);
        return bookingMapper.toResponseList(bookingSpi.findByPlaceId(placeId, null));
    }

    private Place getPlace(@NonNull Long placeId) throws PlaceNotFoundException {
        return placeSpi.findById(placeId)
                .orElseThrow(() -> PlaceNotFoundException.builder()
                        .message("Place not found with id: %s".formatted(placeId))
                        .build());
    }

    private static void controlPlaceApproved(@NonNull Place place) throws BookingPlaceNotApprovedException {
        if (!PlaceStatusEnum.APPROVED.equals(place.getStatus())) {
            throw BookingPlaceNotApprovedException.builder()
                    .message("Place is not available for booking. Current status: %s".formatted(place.getStatus()))
                    .build();
        }
    }

    private static void controlNotOwnPlace(@NonNull Place place, @NonNull User currentUser) throws BookingOwnPlaceException {
        if (isOwner(currentUser, place)) {
            throw BookingOwnPlaceException.builder()
                    .message("You cannot book your own place.")
                    .build();
        }
    }

    private static void controlDates(@NonNull LocalDate startDate, @NonNull LocalDate endDate)
            throws BookingInvalidDatesException {
        if (!endDate.isAfter(startDate)) {
            throw BookingInvalidDatesException.builder()
                    .message("End date must be strictly after start date.")
                    .build();
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days > MAX_BOOKING_DAYS) {
            throw BookingInvalidDatesException.builder()
                    .message("Booking duration cannot exceed %d days.".formatted(MAX_BOOKING_DAYS))
                    .build();
        }
    }

    private void controlAvailability(@NonNull Long placeId, @NonNull LocalDate startDate, @NonNull LocalDate endDate)
            throws BookingUnavailableDatesException {
        if (placeUnavailabilitySpi.hasOverlap(placeId, startDate, endDate)) {
            throw BookingUnavailableDatesException.builder()
                    .message("The requested dates are not available for this place.")
                    .build();
        }
    }

    private static void controlConfirmPermission(@NonNull Booking booking, @NonNull User currentUser) throws UserActionForbiddenException {
        if (!isOwner(currentUser, booking.getPlace()) && !isAdmin(currentUser)) {
            throw UserActionForbiddenException.builder()
                    .message("Only the place owner or an admin can confirm a booking.")
                    .build();
        }
    }

    private static void controlBookingConfirmable(@NonNull Booking booking) throws BookingAlreadyConfirmedException {
        if (!BookingStatusEnum.PENDING.equals(booking.getStatus())) {
            throw BookingAlreadyConfirmedException.builder()
                    .message("Booking cannot be confirmed. Current status: %s".formatted(booking.getStatus()))
                    .build();
        }
    }

    private static void controlCancellationPermission(@NonNull Booking booking, @NonNull User currentUser)
            throws UserActionForbiddenException {
        boolean isBookingOwner = currentUser.getId().equals(booking.getUser().getId());
        if (!isBookingOwner && !isAdmin(currentUser)) {
            throw UserActionForbiddenException.builder()
                    .message("You are not allowed to cancel this booking.")
                    .build();
        }
    }

    private static void controlCancellable(@NonNull Booking booking) throws BookingNotCancellableException {
        if (BookingStatusEnum.CANCELLED.equals(booking.getStatus())
                || BookingStatusEnum.COMPLETED.equals(booking.getStatus())) {
            throw BookingNotCancellableException.builder()
                    .message("Booking cannot be cancelled. Current status: %s".formatted(booking.getStatus()))
                    .build();
        }
    }

    private static void controlPlaceAccess(@NonNull Place place, @NonNull User currentUser) throws UserActionForbiddenException {
        if (!isOwner(currentUser, place) && !isAdmin(currentUser)) {
            throw UserActionForbiddenException.builder()
                    .message("You are not allowed to view bookings for this place.")
                    .build();
        }
    }

    private static Booking buildBooking(@NonNull BookingRequest request, @NonNull Place place, @NonNull User currentUser) {
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate());
        BigDecimal totalPrice = place.getPricePerHour().multiply(BigDecimal.valueOf(days));

        return Booking.builder()
                .user(currentUser)
                .place(place)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(BookingStatusEnum.PENDING)
                .totalPrice(totalPrice)
                .notes(request.notes())
                .build();
    }
}
