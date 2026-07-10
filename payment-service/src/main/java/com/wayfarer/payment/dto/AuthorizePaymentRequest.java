package com.wayfarer.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AuthorizePaymentRequest(
        @NotNull Long bookingId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        // Mock gateway: any value works except the magic "FAIL_CARD" token,
        // which deterministically declines — lets booking-service's Saga
        // compensation path be tested on demand instead of relying on
        // random flakiness.
        @NotBlank String cardToken
) {
}
