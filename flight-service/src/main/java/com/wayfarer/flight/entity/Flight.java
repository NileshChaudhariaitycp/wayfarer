package com.wayfarer.flight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * departureTime/arrivalTime are naive LocalDateTime (no timezone) — a
 * deliberate Phase 3 simplification. A real system needs each airport's
 * timezone to compute flight duration and to search "flights departing
 * August 1st" correctly across timezones; that's out of scope here.
 *
 * seatsAvailable is read/search-only in this phase — nothing decrements it
 * yet. booking-service (Phase 4) will own the pessimistic-locked reservation
 * endpoint that mutates this value inside a single DB transaction, which is
 * why that logic can't live in booking-service's own database: the lock has
 * to be held on the row that actually holds the count.
 */
@Entity
@Table(name = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String flightNumber;

    @Column(nullable = false)
    private String airline;

    @Column(nullable = false)
    private String originAirportCode;

    @Column(nullable = false)
    private String destinationAirportCode;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private int totalSeats;

    @Column(nullable = false)
    private int seatsAvailable;
}
