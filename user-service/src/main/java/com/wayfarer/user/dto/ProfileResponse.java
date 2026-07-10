package com.wayfarer.user.dto;

import com.wayfarer.user.entity.UserProfile;
import java.time.Instant;

public record ProfileResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        Instant createdAt
) {
    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUsername(),
                profile.getEmail(),
                profile.getFullName(),
                profile.getRole(),
                profile.getCreatedAt());
    }
}
