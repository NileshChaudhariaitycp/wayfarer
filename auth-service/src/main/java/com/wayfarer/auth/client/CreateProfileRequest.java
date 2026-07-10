package com.wayfarer.auth.client;

public record CreateProfileRequest(
        Long userId,
        String username,
        String email,
        String fullName,
        String role
) {
}
