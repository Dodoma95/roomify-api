package com.roomify.infrastucture.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.roomify.infrastucture.models.place.PlaceUnavailability;

public interface PlaceUnavailabilityRepository extends JpaRepository<PlaceUnavailability, Long> {

    @Query("SELECT COUNT(u) > 0 FROM PlaceUnavailability u WHERE u.place.id = :placeId AND u.startDate <= :endDate AND u.endDate >= :startDate")
    boolean hasOverlap(@Param("placeId") Long placeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    void deleteByBookingId(Long bookingId);

    List<PlaceUnavailability> findByPlaceId(Long placeId);

    @Query("SELECT u FROM PlaceUnavailability u WHERE u.place.id = :placeId AND u.startDate <= :to AND u.endDate >= :from ORDER BY u.startDate ASC")
    List<PlaceUnavailability> findByPlaceIdAndOverlappingPeriod(
            @Param("placeId") Long placeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
