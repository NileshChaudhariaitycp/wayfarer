package com.wayfarer.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "loyalty-service")
public interface LoyaltyServiceClient {

    @PostMapping("/internal/loyalty/{userId}/earn")
    void earn(@PathVariable("userId") Long userId, @RequestBody EarnRequest request);

    @PostMapping("/internal/loyalty/{userId}/reverse/{bookingId}")
    void reverse(@PathVariable("userId") Long userId, @PathVariable("bookingId") Long bookingId);

    record EarnRequest(Long bookingId, int points) {
    }
}
