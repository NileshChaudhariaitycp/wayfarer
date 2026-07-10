package com.wayfarer.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "flight-service")
public interface FlightServiceClient {

    @GetMapping("/flights/{id}")
    FlightDetails getFlight(@PathVariable("id") Long id);

    @PostMapping("/internal/flights/{id}/reserve")
    void reserveSeats(@PathVariable("id") Long id, @RequestBody SeatsRequest request);

    @PostMapping("/internal/flights/{id}/release")
    void releaseSeats(@PathVariable("id") Long id, @RequestBody SeatsRequest request);

    record SeatsRequest(int seats) {
    }

    record FlightDetails(Long id, String flightNumber, java.math.BigDecimal basePrice, int seatsAvailable) {
    }
}
