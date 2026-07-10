package com.wayfarer.booking.dto;

import com.wayfarer.booking.entity.Booking;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        String bookingType,
        Long customerId,
        Long bookedByUserId,
        Long flightId,
        Long hotelId,
        Long roomTypeId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String status,
        Long paymentId,
        String failureReason,
        Instant createdAt
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingType().name(),
                booking.getCustomerId(),
                booking.getBookedByUserId(),
                booking.getFlightId(),
                booking.getHotelId(),
                booking.getRoomTypeId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getQuantity(),
                booking.getUnitPrice(),
                booking.getTotalPrice(),
                booking.getStatus().name(),
                booking.getPaymentId(),
                booking.getFailureReason(),
                booking.getCreatedAt());
    }
}
