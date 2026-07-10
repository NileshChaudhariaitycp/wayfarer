package com.wayfarer.loyalty.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EarnPointsRequest(
        @NotNull Long bookingId,
        @Min(1) int points
) {
}
