package com.wayfarer.flight.service;

import com.wayfarer.flight.dto.FlightRequest;
import com.wayfarer.flight.dto.FlightResponse;
import com.wayfarer.flight.entity.Flight;
import com.wayfarer.flight.exception.FlightNotFoundException;
import com.wayfarer.flight.repository.FlightRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightService.class);

    private final FlightRepository flightRepository;

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
    public void delete(Long id) {
        if (!flightRepository.existsById(id)) {
            throw new FlightNotFoundException(id);
        }
        flightRepository.deleteById(id);
        log.info("Deleted flight id={}", id);
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
