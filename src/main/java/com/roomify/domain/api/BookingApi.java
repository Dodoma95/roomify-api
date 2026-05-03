package com.roomify.domain.api;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.infrastucture.models.user.User;
import com.roomify.presentation.models.in.BookingFilterInput;
import com.roomify.presentation.models.in.BookingRequest;
import com.roomify.presentation.models.in.PageInfoInput;
import com.roomify.presentation.models.out.BookingPage;
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

public interface BookingApi {

    BookingResponse createBooking(@NonNull BookingRequest request, @NonNull User currentUser)
            throws PlaceNotFoundException, BookingPlaceNotApprovedException, BookingOwnPlaceException,
            BookingInvalidDatesException, BookingUnavailableDatesException;

    BookingResponse confirmBooking(@NonNull Long bookingId, @NonNull User currentUser)
            throws BookingNotFoundException, BookingAlreadyConfirmedException, UserActionForbiddenException;

    void cancelBooking(@NonNull Long bookingId, @NonNull User currentUser)
            throws BookingNotFoundException, BookingNotCancellableException, UserActionForbiddenException;

    List<BookingResponse> getMyBookings(@NonNull User currentUser, @Nullable BookingStatusEnum status);

    List<BookingResponse> getBookingsByPlace(@NonNull Long placeId, @NonNull User currentUser)
            throws PlaceNotFoundException, UserActionForbiddenException;

    @NonNull BookingPage searchBookings(@Nullable BookingFilterInput filter, @NonNull PageInfoInput pagination);
}
