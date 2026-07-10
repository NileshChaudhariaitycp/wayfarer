package com.wayfarer.flight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightRequest(
        @NotBlank String flightNumber,
        @NotBlank String airline,
        @NotBlank String originAirportCode,
        @NotBlank String destinationAirportCode,
        @NotNull @Future LocalDateTime departureTime,
        @NotNull @Future LocalDateTime arrivalTime,
        @NotNull @DecimalMin("0.01") BigDecimal basePrice,
        @Min(1) int totalSeats
) {
}
