package com.wayfarer.user.controller;

import com.wayfarer.user.dto.ProfileResponse;
import com.wayfarer.user.security.GatewayHeaderAuthenticationFilter;
import com.wayfarer.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ProfileResponse me(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER));
        return userProfileService.getById(userId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileResponse getById(@PathVariable Long id) {
        return userProfileService.getById(id);
    }
}
