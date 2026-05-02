package com.roomify.presentation.endpoint;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.roomify.domain.api.PlaceApi;
import com.roomify.infrastucture.models.user.CustomUserDetails;
import com.roomify.presentation.models.in.PlaceRequest;
import com.roomify.presentation.models.in.UpdatePlaceRequest;
import com.roomify.presentation.models.out.PlaceResponse;
import com.roomify.shared.exception.ClientApiException;
import com.roomify.shared.exception.place.CapacityIncoherenteException;
import com.roomify.shared.exception.place.PlaceDescriptionTooShortException;
import com.roomify.shared.exception.place.PlaceDuplicationException;
import com.roomify.shared.exception.place.PlaceNotFoundException;
import com.roomify.shared.exception.place.PlaceStatusInvalidException;
import com.roomify.shared.exception.user.UserActionForbiddenException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Places", description = "Management Places")
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceApi placeApi;

    public PlaceController(PlaceApi placeApi) {
        this.placeApi = placeApi;
    }

    @Operation(
            summary = "Create a new place",
            description = "Allows an authenticated user to create a new place. The place will be pending validation by an admin.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "201",
            description = "Place successfully created",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PlaceResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    @ApiResponse(responseCode = "409", description = "Conflict - Place already exists", content = @Content)
    @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<PlaceResponse> createPlace(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid PlaceRequest request
    ) {
        try {
            PlaceResponse placeResponse = placeApi.create(request, currentUser.user());
            return ResponseEntity.status(HttpStatus.CREATED).body(placeResponse);
        } catch (PlaceDuplicationException e) {
            throw ClientApiException.ofConflict(e.getMessage(), e);
        } catch (CapacityIncoherenteException | PlaceDescriptionTooShortException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Update an existing place",
            description = "Allows the owner or an admin to partially update a place. Only provided fields are updated. If the place was APPROVED, it is reset to PENDING for re-validation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Place successfully updated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Forbidden - Not the owner of this place", content = @Content)
    @ApiResponse(responseCode = "404", description = "Place not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Conflict - Another place with the same name and address already exists", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OWNER')")
    public ResponseEntity<PlaceResponse> updatePlace(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid UpdatePlaceRequest request
    ) {
        try {
            PlaceResponse placeResponse = placeApi.update(id, request, currentUser.user());
            return ResponseEntity.ok(placeResponse);
        } catch (PlaceNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        } catch (PlaceDuplicationException e) {
            throw ClientApiException.ofConflict(e.getMessage(), e);
        } catch (CapacityIncoherenteException | PlaceDescriptionTooShortException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Approve a place",
            description = "Allows an admin to approve a PENDING place. Only places in PENDING status can be approved.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Place successfully approved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceResponse.class)))
    @ApiResponse(responseCode = "400", description = "Place is not in PENDING status", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    @ApiResponse(responseCode = "404", description = "Place not found", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PlaceResponse> approvePlace(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(placeApi.approve(id));
        } catch (PlaceNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (PlaceStatusInvalidException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Reject a place",
            description = "Allows an admin to reject a PENDING or APPROVED place.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Place successfully rejected",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlaceResponse.class)))
    @ApiResponse(responseCode = "400", description = "Place is already REJECTED", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    @ApiResponse(responseCode = "404", description = "Place not found", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PlaceResponse> rejectPlace(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(placeApi.reject(id));
        } catch (PlaceNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (PlaceStatusInvalidException e) {
            throw ClientApiException.ofBadRequest(e.getMessage(), e);
        }
    }

    @Operation(
            summary = "Delete a place",
            description = "Allows the owner or an admin to permanently delete a place.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "204", description = "Place successfully deleted", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Forbidden - Not the owner of this place", content = @Content)
    @ApiResponse(responseCode = "404", description = "Place not found", content = @Content)
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OWNER')")
    public ResponseEntity<Void> deletePlace(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            placeApi.delete(id, currentUser.user());
            return ResponseEntity.noContent().build();
        } catch (PlaceNotFoundException e) {
            throw ClientApiException.ofNotFound(e.getMessage(), e);
        } catch (UserActionForbiddenException e) {
            throw ClientApiException.ofForbidden(e.getMessage(), e);
        }
    }
}
