package com.wayfarer.payment.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(Instant.now(), status, message, path);
    }
}
