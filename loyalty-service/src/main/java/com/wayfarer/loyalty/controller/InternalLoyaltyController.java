package com.wayfarer.loyalty.controller;

import com.wayfarer.loyalty.dto.EarnPointsRequest;
import com.wayfarer.loyalty.dto.LoyaltyAccountResponse;
import com.wayfarer.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only — called directly by booking-service, never routed through the gateway. */
@RestController
@RequestMapping("/internal/loyalty/{userId}")
@RequiredArgsConstructor
public class InternalLoyaltyController {

    private final LoyaltyService loyaltyService;

    @PostMapping("/earn")
    public LoyaltyAccountResponse earn(@PathVariable Long userId, @Valid @RequestBody EarnPointsRequest request) {
        return loyaltyService.earn(userId, request);
    }

    @PostMapping("/reverse/{bookingId}")
    public LoyaltyAccountResponse reverse(@PathVariable Long userId, @PathVariable Long bookingId) {
        return loyaltyService.reverse(userId, bookingId);
    }
}
