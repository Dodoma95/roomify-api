package com.roomify.infrastucture.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.roomify.domain.models.BookingStatusEnum;
import com.roomify.infrastucture.models.booking.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatusEnum status);

    List<Booking> findByPlaceId(Long placeId);

    List<Booking> findByPlaceIdAndStatus(Long placeId, BookingStatusEnum status);

    List<Booking> findByStatusAndEndDateBefore(BookingStatusEnum status, LocalDate date);
}
