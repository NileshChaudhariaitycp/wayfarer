package com.wayfarer.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nullable flight/hotel fields model both booking types in one table rather
 * than table-per-type — a deliberate simplification for a learning project.
 * A larger real system might split this into FlightBooking/HotelBooking (or
 * a JSON "details" column) once the two types' fields diverge further; here
 * they're similar enough that one table stays readable.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingType bookingType;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long bookedByUserId;

    private Long flightId;

    private Long hotelId;

    private Long roomTypeId;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    private Long paymentId;

    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
