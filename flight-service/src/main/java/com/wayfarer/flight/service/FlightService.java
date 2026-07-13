package com.wayfarer.flight.service;

import com.wayfarer.flight.dto.FlightRequest;
import com.wayfarer.flight.dto.FlightResponse;
import com.wayfarer.flight.entity.Flight;
import com.wayfarer.flight.exception.FlightNotFoundException;
import com.wayfarer.flight.exception.InsufficientSeatsException;
import com.wayfarer.flight.repository.FlightRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Search results are cached (see application.yml for TTL — short, and only
 * backed by Redis under the "docker" env; local dev uses Spring's default
 * in-memory cache). Deliberately NOT evicted by reserveSeats/releaseSeats —
 * a stale cached seat count can only ever show a seat as available that
 * turns out to be gone by the time you try to book it, which
 * booking-service's pessimistic-locked reserveSeats already re-validates
 * live and correctly rejects (see ADR 0005). That's the same "someone beat
 * you to it" experience any booking site has; it can never cause
 * overbooking, since the cache is never in the actual reservation path.
 */
@Service
@RequiredArgsConstructor
public class FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightService.class);

    private final FlightRepository flightRepository;

    @Cacheable(value = "flightSearch", key = "#origin + '-' + #destination + '-' + #date")
    public List<FlightResponse> search(String origin, String destination, LocalDate date) {
        List<Flight> flights;
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            flights = flightRepository
                    .findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCaseAndDepartureTimeBetween(
                            origin, destination, start, end);
        } else {
            flights = flightRepository.findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCase(
                    origin, destination);
        }
        return flights.stream().map(FlightResponse::from).toList();
    }

    public FlightResponse getById(Long id) {
        return flightRepository.findById(id)
                .map(FlightResponse::from)
                .orElseThrow(() -> new FlightNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "flightSearch", allEntries = true)
    public FlightResponse create(FlightRequest request) {
        Flight flight = new Flight();
        applyRequest(flight, request);
        flight.setSeatsAvailable(request.totalSeats());
        flight = flightRepository.save(flight);
        log.info("Created flight {} ({} -> {})", flight.getFlightNumber(),
                flight.getOriginAirportCode(), flight.getDestinationAirportCode());
        return FlightResponse.from(flight);
    }

    @Transactional
    @CacheEvict(value = "flightSearch", allEntries = true)
    public FlightResponse update(Long id, FlightRequest request) {
        Flight flight = flightRepository.findById(id).orElseThrow(() -> new FlightNotFoundException(id));
        int seatsBooked = flight.getTotalSeats() - flight.getSeatsAvailable();
        applyRequest(flight, request);
        flight.setSeatsAvailable(Math.max(0, request.totalSeats() - seatsBooked));
        flight = flightRepository.save(flight);
        log.info("Updated flight {}", flight.getFlightNumber());
        return FlightResponse.from(flight);
    }

    @Transactional
    @CacheEvict(value = "flightSearch", allEntries = true)
    public void delete(Long id) {
        if (!flightRepository.existsById(id)) {
            throw new FlightNotFoundException(id);
        }
        flightRepository.deleteById(id);
        log.info("Deleted flight id={}", id);
    }

    // Unlike loyalty-service's earn/reverse, this isn't idempotent against a
    // duplicate call — calling release twice adds seats back twice. That's
    // acceptable only because booking-service (the only caller) tracks each
    // booking's status and guarantees it calls release at most once per
    // reservation. If a second caller of this internal API appeared, it
    // would need the same bookingId-keyed idempotency check loyalty-service
    // uses.
    @Transactional
    public void reserveSeats(Long flightId, int seats) {
        Flight flight = flightRepository.findByIdForUpdate(flightId)
                .orElseThrow(() -> new FlightNotFoundException(flightId));
        if (flight.getSeatsAvailable() < seats) {
            throw new InsufficientSeatsException(flightId, seats, flight.getSeatsAvailable());
        }
        flight.setSeatsAvailable(flight.getSeatsAvailable() - seats);
        flightRepository.save(flight);
        log.info("Reserved {} seat(s) on flight {} ({} remaining)", seats, flightId, flight.getSeatsAvailable());
    }

    @Transactional
    public void releaseSeats(Long flightId, int seats) {
        Flight flight = flightRepository.findByIdForUpdate(flightId)
                .orElseThrow(() -> new FlightNotFoundException(flightId));
        flight.setSeatsAvailable(Math.min(flight.getTotalSeats(), flight.getSeatsAvailable() + seats));
        flightRepository.save(flight);
        log.info("Released {} seat(s) on flight {} ({} now available)", seats, flightId, flight.getSeatsAvailable());
    }

    private void applyRequest(Flight flight, FlightRequest request) {
        flight.setFlightNumber(request.flightNumber());
        flight.setAirline(request.airline());
        flight.setOriginAirportCode(request.originAirportCode());
        flight.setDestinationAirportCode(request.destinationAirportCode());
        flight.setDepartureTime(request.departureTime());
        flight.setArrivalTime(request.arrivalTime());
        flight.setBasePrice(request.basePrice());
        flight.setTotalSeats(request.totalSeats());
    }
}
