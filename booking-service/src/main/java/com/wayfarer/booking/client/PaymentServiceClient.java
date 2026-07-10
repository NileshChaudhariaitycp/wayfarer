package com.wayfarer.booking.client;

import java.math.BigDecimal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @PostMapping("/internal/payments/authorize")
    PaymentResult authorize(@RequestBody AuthorizeRequest request);

    @PostMapping("/internal/payments/{id}/refund")
    PaymentResult refund(@PathVariable("id") Long id);

    record AuthorizeRequest(Long bookingId, BigDecimal amount, String cardToken) {
    }

    record PaymentResult(Long id, Long bookingId, BigDecimal amount, String status) {
    }
}
