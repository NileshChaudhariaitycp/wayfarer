package com.wayfarer.loyalty.controller;

import com.wayfarer.loyalty.dto.LoyaltyAccountResponse;
import com.wayfarer.loyalty.security.GatewayHeaderAuthenticationFilter;
import com.wayfarer.loyalty.service.LoyaltyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/me")
    public LoyaltyAccountResponse me(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER));
        return loyaltyService.getBalance(userId);
    }
}
