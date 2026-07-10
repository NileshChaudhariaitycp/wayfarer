package com.wayfarer.flight.repository;

import com.wayfarer.flight.entity.Flight;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCaseAndDepartureTimeBetween(
            String originAirportCode, String destinationAirportCode, LocalDateTime from, LocalDateTime to);

    List<Flight> findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCase(
            String originAirportCode, String destinationAirportCode);
}
