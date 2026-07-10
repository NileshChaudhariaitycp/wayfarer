package com.wayfarer.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-registration always creates a CUSTOMER — there is no role field here
 * by design. ADMIN and TRAVEL_AGENT accounts are provisioned out-of-band
 * (seeded now; an admin-only "create user with role" endpoint is a Phase 5
 * hardening concern, not something a public endpoint should ever accept).
 */
public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 1, max = 100)
        String fullName
) {
}
