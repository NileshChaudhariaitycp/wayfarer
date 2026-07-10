package com.wayfarer.flight.controller;

import com.wayfarer.flight.dto.SeatsRequest;
import com.wayfarer.flight.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only — called directly by booking-service, never routed
 * through the gateway. Pessimistic-locked mutation of seatsAvailable lives
 * here (not in booking-service) because the lock only holds within the DB
 * transaction/connection that owns the row — see ADR 0005.
 */
@RestController
@RequestMapping("/internal/flights")
@RequiredArgsConstructor
public class InternalFlightController {

    private final FlightService flightService;

    @PostMapping("/{id}/reserve")
    public void reserve(@PathVariable Long id, @Valid @RequestBody SeatsRequest request) {
        flightService.reserveSeats(id, request.seats());
    }

    @PostMapping("/{id}/release")
    public void release(@PathVariable Long id, @Valid @RequestBody SeatsRequest request) {
        flightService.releaseSeats(id, request.seats());
    }
}
