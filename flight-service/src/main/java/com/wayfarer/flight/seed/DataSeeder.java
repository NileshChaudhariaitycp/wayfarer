package com.wayfarer.flight.seed;

import com.wayfarer.flight.entity.Flight;
import com.wayfarer.flight.repository.FlightRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Departure times are computed relative to "now" (next few days) rather than
 * hardcoded dates, so search-by-date demos keep working no matter when this
 * seeder actually runs — a hardcoded past date would silently stop matching
 * any "future flights" search a week after this code was written.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private record Route(String airline, String origin, String destination, int durationHours, BigDecimal basePrice) {
    }

    private static final Route[] ROUTES = {
            new Route("Wayfarer Air", "JFK", "LAX", 6, new BigDecimal("289.99")),
            new Route("Wayfarer Air", "LAX", "JFK", 5, new BigDecimal("279.99")),
            new Route("Wayfarer Air", "ORD", "SFO", 4, new BigDecimal("219.50")),
            new Route("Wayfarer Air", "SFO", "ORD", 4, new BigDecimal("229.50")),
            new Route("Wayfarer Air", "ATL", "MIA", 2, new BigDecimal("129.00")),
            new Route("Wayfarer Air", "JFK", "LHR", 7, new BigDecimal("459.00")),
    };

    private final FlightRepository flightRepository;

    public DataSeeder(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public void run(String... args) {
        if (flightRepository.count() > 0) {
            log.info("Flights already seeded, skipping.");
            return;
        }

        LocalDateTime baseDate = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0).withSecond(0).withNano(0);
        int flightCounter = 100;
        int seeded = 0;

        for (Route route : ROUTES) {
            for (int dayOffset = 0; dayOffset < 3; dayOffset++) {
                Flight flight = new Flight();
                flight.setFlightNumber("WF" + (flightCounter++));
                flight.setAirline(route.airline());
                flight.setOriginAirportCode(route.origin());
                flight.setDestinationAirportCode(route.destination());
                LocalDateTime departure = baseDate.plusDays(dayOffset);
                flight.setDepartureTime(departure);
                flight.setArrivalTime(departure.plusHours(route.durationHours()));
                flight.setBasePrice(route.basePrice());
                flight.setTotalSeats(150);
                flight.setSeatsAvailable(150);
                flightRepository.save(flight);
                seeded++;
            }
        }

        log.info("Seeded {} demo flights across {} routes.", seeded, ROUTES.length);
    }
}
