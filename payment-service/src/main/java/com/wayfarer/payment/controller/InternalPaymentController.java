package com.wayfarer.payment.controller;

import com.wayfarer.payment.dto.AuthorizePaymentRequest;
import com.wayfarer.payment.dto.PaymentResponse;
import com.wayfarer.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only — called directly by booking-service, never routed through the gateway. */
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/authorize")
    public ResponseEntity<PaymentResponse> authorize(@Valid @RequestBody AuthorizePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.authorize(request));
    }

    @PostMapping("/{id}/capture")
    public PaymentResponse capture(@PathVariable Long id) {
        return paymentService.capture(id);
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable Long id) {
        return paymentService.refund(id);
    }
}
