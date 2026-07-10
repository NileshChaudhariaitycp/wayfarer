package com.wayfarer.loyalty.dto;

import jakarta.validation.constraints.NotNull;

public record ReversePointsRequest(
        @NotNull Long bookingId
) {
}
