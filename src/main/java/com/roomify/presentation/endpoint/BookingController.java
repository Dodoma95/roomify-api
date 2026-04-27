package com.roomify.presentation.endpoint;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.roomify.domain.api.BookingApi;
import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.infrastucture.models.user.CustomUserDetails;
import com.roomify.presentation.models.in.BookingRequest;
import com.roomify.presentation.models.out.BookingResponse;
import com.roomify.shared.exception.ClientApiException;
import com.roomify.shared.exception.booking.BookingAlreadyConfirmedException;
import com.roomify.shared.exception.booking.BookingInvalidDatesException;
import com.roomify.shared.exception.booking.BookingNotCancellableException;
import com.roomify.shared.exception.booking.BookingNotFoundException;
import com.roomify.shared.exception.booking.BookingOwnPlaceException;
import com.roomify.shared.exception.booking.BookingPlaceNotApprovedException;
import com.roomify.shared.exception.booking.BookingUnavailableDatesException;
import com.roomify.shared.exception.place.PlaceNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Bookings", description = "Booking management")
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingApi bookingApi;

    public BookingController(BookingApi bookingApi) {
        this.bookingApi = bookingApi;
    }

    @Operation(
            summary = "Book a place",
            description = "Creates a confirmed booking for a place. The place must be APPROVED, the requested dates must be available, and the user cannot book their own place.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "201", description = "Booking successfully created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid dates or validation error", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Cannot book own place", content = @Content)
    @ApiResponse(responseCode = "404", description = "Place not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Dates unavailable or place not approved", content = @Content)
    @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid BookingRequest request
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(bookingApi.createBooking(request, currentUser.user()));
        } catch (PlaceNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (BookingOwnPlaceException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        } catch (BookingPlaceNotApprovedException | BookingUnavailableDatesException e) {
            throw ClientApiException.ofConflict(e.getMessage(), e);
        } catch (BookingInvalidDatesException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Confirm a booking",
            description = "Confirms a pending booking. Only the place owner or an admin can confirm.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Booking successfully confirmed",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "400", description = "Booking already confirmed or not in PENDING state", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not allowed to confirm this booking", content = @Content)
    @ApiResponse(responseCode = "404", description = "Booking not found", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            return ResponseEntity.ok(bookingApi.confirmBooking(id, currentUser.user()));
        } catch (BookingNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        } catch (BookingAlreadyConfirmedException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Cancel a booking",
            description = "Cancels a booking. The user can cancel their own booking; admins can cancel any booking.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "204", description = "Booking successfully cancelled", content = @Content)
    @ApiResponse(responseCode = "400", description = "Booking already cancelled", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not allowed to cancel this booking", content = @Content)
    @ApiResponse(responseCode = "404", description = "Booking not found", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            bookingApi.cancelBooking(id, currentUser.user());
            return ResponseEntity.noContent().build();
        } catch (BookingNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        } catch (BookingNotCancellableException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "List my bookings",
            description = "Returns all bookings made by the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "List of bookings",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) BookingStatusEnum status
    ) {
        return ResponseEntity.ok(bookingApi.getMyBookings(currentUser.user(), status));
    }

    @Operation(
            summary = "List bookings for a place",
            description = "Returns all bookings for a given place. Only accessible by the place owner or an admin.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "List of bookings for the place",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not the owner of this place", content = @Content)
    @ApiResponse(responseCode = "404", description = "Place not found", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @GetMapping("/place/{placeId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<BookingResponse>> getBookingsByPlace(
            @PathVariable Long placeId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            return ResponseEntity.ok(bookingApi.getBookingsByPlace(placeId, currentUser.user()));
        } catch (PlaceNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        }
    }
}
