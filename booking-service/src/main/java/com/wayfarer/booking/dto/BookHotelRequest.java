package com.wayfarer.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookHotelRequest(
        @NotNull Long hotelId,
        @NotNull Long roomTypeId,
        @NotNull @Future LocalDate checkInDate,
        @NotNull @Future LocalDate checkOutDate,
        @Min(1) int rooms,
        Long customerId,
        @NotBlank String cardToken
) {
}
