package com.wayfarer.flight.dto;

import jakarta.validation.constraints.Min;

public record SeatsRequest(@Min(1) int seats) {
}
