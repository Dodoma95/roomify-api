package com.roomify.infrastucture.adapter;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.roomify.domain.models.PlaceStatusEnum;
import com.roomify.domain.models.PlaceTypeEnum;
import com.roomify.infrastucture.models.place.Place;

class PlaceSpecifications {

    private PlaceSpecifications() {}

    static Specification<Place> hasTypes(List<PlaceTypeEnum> types) {
        return (root, query, cb) -> root.get("type").in(types);
    }

    static Specification<Place> hasStatuses(List<PlaceStatusEnum> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    static Specification<Place> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.strip().toLowerCase() + "%");
    }

    static Specification<Place> hasOwner(Long ownerId) {
        return (root, query, cb) ->
                cb.equal(root.get("owner").get("id"), ownerId);
    }

    static Specification<Place> capacityAtLeast(int min) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("capacity"), min);
    }

    static Specification<Place> capacityAtMost(int max) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("capacity"), max);
    }

    static Specification<Place> priceAtLeast(BigDecimal min) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("pricePerHour"), min);
    }

    static Specification<Place> priceAtMost(BigDecimal max) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("pricePerHour"), max);
    }
}
