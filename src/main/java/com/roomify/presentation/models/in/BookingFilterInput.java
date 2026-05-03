package com.roomify.presentation.models.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.domain.models.PlaceStatusEnum;
import com.roomify.domain.models.PlaceTypeEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingFilterInput {

    @Schema(description = "Filter by booking statuses", example = "[\"PENDING\", \"CONFIRMED\"]")
    @Nullable List<BookingStatusEnum> statuses;

    @Schema(description = "Booking start date >= this value (YYYY-MM-DD)", example = "2025-06-01")
    @Nullable LocalDate startDateFrom;

    @Schema(description = "Booking start date <= this value (YYYY-MM-DD)", example = "2025-06-30")
    @Nullable LocalDate startDateTo;

    @Schema(description = "Booking end date >= this value (YYYY-MM-DD)", example = "2025-06-05")
    @Nullable LocalDate endDateFrom;

    @Schema(description = "Booking end date <= this value (YYYY-MM-DD)", example = "2025-07-31")
    @Nullable LocalDate endDateTo;

    @Schema(description = "Total price minimum (inclusive, in euros)", example = "50.0")
    @Nullable Double totalPriceMin;

    @Schema(description = "Total price maximum (inclusive, in euros)", example = "500.0")
    @Nullable Double totalPriceMax;

    @Schema(description = "Created at >= this timestamp (ISO-8601)", example = "2025-01-01T00:00:00Z")
    @Nullable Instant createdAtFrom;

    @Schema(description = "Created at <= this timestamp (ISO-8601)", example = "2025-12-31T23:59:59Z")
    @Nullable Instant createdAtTo;

    @Schema(description = "Filter by partial match on notes (case-insensitive)", example = "quiet")
    @Nullable String notesContains;

    @Schema(description = "Filter by exact place identifier", example = "42")
    @Nullable Long placeId;

    @Schema(description = "Filter by partial match on place name (case-insensitive)", example = "meeting")
    @Nullable String placeNameContains;

    @Schema(description = "Filter by place types", example = "[\"MEETING_ROOM\"]")
    @Nullable List<PlaceTypeEnum> placeTypes;

    @Schema(description = "Filter by place validation statuses", example = "[\"APPROVED\"]")
    @Nullable List<PlaceStatusEnum> placeStatuses;

    @Schema(description = "Filter by exact booker user identifier", example = "7")
    @Nullable Long userId;

    @Schema(description = "Filter by partial match on booker email (case-insensitive)", example = "@gmail")
    @Nullable String userEmailContains;

    @Schema(description = "Filter by exact place owner identifier", example = "3")
    @Nullable Long ownerId;
}
