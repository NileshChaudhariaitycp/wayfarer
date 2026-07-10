package com.wayfarer.hotel.dto;

import jakarta.validation.constraints.Min;

public record RoomsRequest(@Min(1) int rooms) {
}
