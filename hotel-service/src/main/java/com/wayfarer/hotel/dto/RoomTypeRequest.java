package com.wayfarer.hotel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RoomTypeRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.01") BigDecimal pricePerNight,
        @Min(1) int totalRooms
) {
}
