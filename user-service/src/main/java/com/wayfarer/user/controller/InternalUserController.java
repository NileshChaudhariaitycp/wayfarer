package com.wayfarer.user.controller;

import com.wayfarer.user.dto.CreateProfileRequest;
import com.wayfarer.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only. Called by auth-service's Feign client right after
 * registration — never exposed through api-gateway's public routes.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<Void> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        userProfileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
