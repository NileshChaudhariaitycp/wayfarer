package com.wayfarer.auth.dto;

public record AuthResponse(
        String token,
        String username,
        String role,
        long expiresAtEpochMillis
) {
}
