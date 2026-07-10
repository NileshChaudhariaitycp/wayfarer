package com.wayfarer.payment.service;

import com.wayfarer.payment.dto.AuthorizePaymentRequest;
import com.wayfarer.payment.dto.PaymentResponse;
import com.wayfarer.payment.entity.Payment;
import com.wayfarer.payment.entity.PaymentStatus;
import com.wayfarer.payment.exception.PaymentDeclinedException;
import com.wayfarer.payment.exception.PaymentNotFoundException;
import com.wayfarer.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String DECLINE_TOKEN = "FAIL_CARD";

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse authorize(AuthorizePaymentRequest request) {
        Payment payment = new Payment();
        payment.setBookingId(request.bookingId());
        payment.setAmount(request.amount());

        if (DECLINE_TOKEN.equals(request.cardToken())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment declined for booking {} amount {}", request.bookingId(), request.amount());
            throw new PaymentDeclinedException();
        }

        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment = paymentRepository.save(payment);
        log.info("Payment {} authorized for booking {} amount {}", payment.getId(), request.bookingId(), request.amount());
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse capture(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
        payment.setStatus(PaymentStatus.CAPTURED);
        payment = paymentRepository.save(payment);
        log.info("Payment {} captured", payment.getId());
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse refund(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            // Idempotent: a retried compensating call must not double-refund.
            log.info("Payment {} already refunded, skipping.", id);
            return PaymentResponse.from(payment);
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);
        log.info("Payment {} refunded", payment.getId());
        return PaymentResponse.from(payment);
    }
}
