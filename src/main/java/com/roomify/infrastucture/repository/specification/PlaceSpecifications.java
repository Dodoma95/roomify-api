package com.roomify.infrastucture.repository.specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.domain.models.PlaceStatusEnum;
import com.roomify.domain.models.PlaceTypeEnum;
import com.roomify.infrastucture.models.booking.Booking;
import com.roomify.infrastucture.models.place.Place;
import com.roomify.infrastucture.models.place.PlaceUnavailability;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlaceSpecifications {

    public static Specification<Place> hasTypes(List<PlaceTypeEnum> types) {
        return (root, query, cb) -> root.get("type").in(types);
    }

    public static Specification<Place> hasStatuses(List<PlaceStatusEnum> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Place> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.strip().toLowerCase() + "%");
    }

    public static Specification<Place> hasOwner(Long ownerId) {
        return (root, query, cb) ->
                cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Place> capacityAtLeast(int min) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("capacity"), min);
    }

    public static Specification<Place> capacityAtMost(int max) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("capacity"), max);
    }

    public static Specification<Place> priceAtLeast(BigDecimal min) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("pricePerHour"), min);
    }

    public static Specification<Place> priceAtMost(BigDecimal max) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("pricePerHour"), max);
    }

    /**
     * Exclut les places ayant une réservation active (PENDING ou CONFIRMED) ou une
     * indisponibilité déclarée qui chevauche la période [from, to].
     * La condition de chevauchement est : existing.start <= to AND existing.end >= from.
     */
    public static Specification<Place> isAvailableBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            Subquery<Long> bookingSub = query.subquery(Long.class);
            Root<Booking> bRoot = bookingSub.from(Booking.class);
            bookingSub.select(bRoot.get("id")).where(cb.and(
                    cb.equal(bRoot.get("place").get("id"), root.get("id")),
                    bRoot.get("status").in(List.of(BookingStatusEnum.CONFIRMED, BookingStatusEnum.PENDING)),
                    cb.lessThanOrEqualTo(bRoot.<LocalDate>get("startDate"), to),
                    cb.greaterThanOrEqualTo(bRoot.<LocalDate>get("endDate"), from)
            ));

            Subquery<Long> unavailSub = query.subquery(Long.class);
            Root<PlaceUnavailability> uRoot = unavailSub.from(PlaceUnavailability.class);
            unavailSub.select(uRoot.get("id")).where(cb.and(
                    cb.equal(uRoot.get("place").get("id"), root.get("id")),
                    cb.lessThanOrEqualTo(uRoot.<LocalDate>get("startDate"), to),
                    cb.greaterThanOrEqualTo(uRoot.<LocalDate>get("endDate"), from)
            ));

            return cb.and(
                    cb.not(cb.exists(bookingSub)),
                    cb.not(cb.exists(unavailSub))
            );
        };
    }
}
