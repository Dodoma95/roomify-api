package com.roomify.infrastucture.repository.specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.domain.models.PlaceStatusEnum;
import com.roomify.domain.models.PlaceTypeEnum;
import com.roomify.infrastucture.models.booking.Booking;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BookingSpecifications {

    private static final String PLACE = "place";

    public static Specification<Booking> hasStatuses(List<BookingStatusEnum> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Booking> startDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    public static Specification<Booking> startDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }

    public static Specification<Booking> endDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endDate"), from);
    }

    public static Specification<Booking> endDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), to);
    }

    public static Specification<Booking> totalPriceMin(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("totalPrice"), min);
    }

    public static Specification<Booking> totalPriceMax(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("totalPrice"), max);
    }

    public static Specification<Booking> createdAtFrom(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Booking> createdAtTo(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<Booking> notesContains(String value) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("notes")), "%" + value.strip().toLowerCase() + "%");
    }

    public static Specification<Booking> hasPlaceId(Long placeId) {
        return (root, query, cb) -> cb.equal(root.get(PLACE).get("id"), placeId);
    }

    public static Specification<Booking> placeNameContains(String value) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(PLACE).get("name")), "%" + value.strip().toLowerCase() + "%");
    }

    public static Specification<Booking> hasPlaceTypes(List<PlaceTypeEnum> types) {
        return (root, query, cb) -> root.get(PLACE).get("type").in(types);
    }

    public static Specification<Booking> hasPlaceStatuses(List<PlaceStatusEnum> statuses) {
        return (root, query, cb) -> root.get(PLACE).get("status").in(statuses);
    }

    public static Specification<Booking> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Booking> userEmailContains(String value) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("user").get("email")), "%" + value.strip().toLowerCase() + "%");
    }

    public static Specification<Booking> hasOwnerId(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get(PLACE).get("owner").get("id"), ownerId);
    }
}
