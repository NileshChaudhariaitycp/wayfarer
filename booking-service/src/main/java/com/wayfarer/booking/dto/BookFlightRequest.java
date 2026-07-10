package com.wayfarer.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * customerId is optional: a CUSTOMER booking for themselves omits it (the
 * service fills in their own id and ignores anything else supplied here); a
 * TRAVEL_AGENT or ADMIN booking on someone else's behalf must supply it.
 */
public record BookFlightRequest(
        @NotNull Long flightId,
        @Min(1) int seats,
        Long customerId,
        @NotBlank String cardToken
) {
}
