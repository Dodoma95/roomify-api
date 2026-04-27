package com.roomify.infrastucture.models.place;

import java.time.LocalDate;

import com.roomify.domain.models.UnavailabilityReasonEnum;
import com.roomify.infrastucture.models.booking.Booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "roomify", name = "place_unavailability")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceUnavailability {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "place_unavailability_seq_gen")
    @SequenceGenerator(name = "place_unavailability_seq_gen", sequenceName = "place_unavailability_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnavailabilityReasonEnum reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
