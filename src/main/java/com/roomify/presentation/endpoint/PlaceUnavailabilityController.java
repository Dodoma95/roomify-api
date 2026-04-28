package com.roomify.presentation.endpoint;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.roomify.domain.api.PlaceUnavailabilityApi;
import com.roomify.infrastucture.models.user.CustomUserDetails;
import com.roomify.presentation.models.in.BlockDatesRequest;
import com.roomify.presentation.models.out.PlaceUnavailabilityResponse;
import com.roomify.shared.exception.ClientApiException;
import com.roomify.shared.exception.booking.BookingInvalidDatesException;
import com.roomify.shared.exception.booking.BookingUnavailableDatesException;
import com.roomify.shared.exception.booking.PlaceUnavailabilityNotFoundException;
import com.roomify.shared.exception.place.PlaceNotFoundException;
import com.roomify.shared.exception.user.UserActionForbiddenException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Place Unavailability", description = "Manage unavailability periods for places")
@RestController
@RequestMapping("/api/v1/places/{placeId}/unavailability")
public class PlaceUnavailabilityController {

    private final PlaceUnavailabilityApi placeUnavailabilityApi;

    public PlaceUnavailabilityController(PlaceUnavailabilityApi placeUnavailabilityApi) {
        this.placeUnavailabilityApi = placeUnavailabilityApi;
    }

    @Operation(
            summary = "List unavailability periods for a place",
            description = "Returns all unavailability periods (bookings and owner-blocked) for a given place.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "List of unavailability periods",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceUnavailabilityResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not the owner of this place", content = @Content)
    @ApiResponse(responseCode = "404", description = "Place not found", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<PlaceUnavailabilityResponse>> getPlaceUnavailability(
            @PathVariable Long placeId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            return ResponseEntity.ok(placeUnavailabilityApi.getPlaceUnavailability(placeId, currentUser.user()));
        } catch (PlaceNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Block dates on a place",
            description = "Allows a place owner or admin to manually block a date range, preventing bookings during that period.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "201", description = "Dates successfully blocked",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceUnavailabilityResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid dates", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not the owner of this place", content = @Content)
    @ApiResponse(responseCode = "404", description = "Place not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Dates overlap with an existing booking or blocked period", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @PostMapping("/block")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PlaceUnavailabilityResponse> blockDates(
            @PathVariable Long placeId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid BlockDatesRequest request
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(placeUnavailabilityApi.blockDates(placeId, request, currentUser.user()));
        } catch (PlaceNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        } catch (BookingUnavailableDatesException e) {
            throw ClientApiException.ofConflict(e.getMessage(), e);
        } catch (BookingInvalidDatesException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Unblock dates on a place",
            description = "Removes an OWNER_BLOCKED unavailability entry. Cannot remove booking-linked entries — cancel the booking instead.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "204", description = "Dates successfully unblocked", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Not the owner or entry is booking-linked", content = @Content)
    @ApiResponse(responseCode = "404", description = "Unavailability entry not found", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @DeleteMapping("/{unavailabilityId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> unblockDates(
            @PathVariable Long placeId,
            @PathVariable Long unavailabilityId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            placeUnavailabilityApi.unblockDates(placeId, unavailabilityId, currentUser.user());
            return ResponseEntity.noContent().build();
        } catch (PlaceNotFoundException | PlaceUnavailabilityNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        }
    }
}
