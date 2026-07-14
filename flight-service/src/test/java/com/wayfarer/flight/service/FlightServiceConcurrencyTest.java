package com.wayfarer.flight.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wayfarer.flight.entity.Flight;
import com.wayfarer.flight.exception.InsufficientSeatsException;
import com.wayfarer.flight.repository.FlightRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves the pessimistic write lock behind reserveSeats (ADR 0005) actually
 * does its job under real concurrency — not just that the code compiles.
 *
 * Without FlightRepository.findByIdForUpdate's SELECT ... FOR UPDATE, N
 * concurrent requests could each read the same "seatsAvailable" value
 * before any of them writes back, collectively overselling the flight (the
 * classic lost-update race). A mocked repository can't reproduce real
 * row-level locking, and H2 doesn't enforce SELECT ... FOR UPDATE blocking
 * the same way Postgres does — this needs the real thing, hence
 * Testcontainers.
 *
 * @AutoConfigureTestDatabase(replace = NONE) stops @DataJpaTest from doing
 * its normal trick of silently swapping in an embedded H2 DataSource, which
 * would otherwise defeat the entire point of this test.
 *
 * @Transactional(propagation = NOT_SUPPORTED) stops @DataJpaTest's OTHER
 * normal trick — wrapping each test method in one transaction that's rolled
 * back at the end — since that would put every thread's reserveSeats() call
 * inside (or fighting over) the test method's own ambient transaction
 * instead of each getting its own independent one, exactly like production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
class FlightServiceConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private FlightRepository flightRepository;

    @Test
    void concurrentReserveSeats_neverOversellsBeyondSeatsAvailable() throws InterruptedException {
        FlightService flightService = new FlightService(flightRepository);

        Flight flight = new Flight();
        flight.setFlightNumber("WF-TEST-CONCURRENCY");
        flight.setAirline("Wayfarer Air");
        flight.setOriginAirportCode("JFK");
        flight.setDestinationAirportCode("LAX");
        flight.setDepartureTime(LocalDateTime.now().plusDays(1));
        flight.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(6));
        flight.setBasePrice(new BigDecimal("100.00"));
        flight.setTotalSeats(10);
        flight.setSeatsAvailable(10);
        flight = flightRepository.saveAndFlush(flight);
        Long flightId = flight.getId();

        int threadCount = 20; // 20 one-seat requests competing for only 10 real seats
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startLine.await(); // hold every thread here so they all fire together
                    flightService.reserveSeats(flightId, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientSeatsException expected) {
                    rejectedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown(); // release all 20 threads at once
        boolean completedInTime = finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(completedInTime).as("all threads should finish, not deadlock").isTrue();
        assertThat(successCount.get()).isEqualTo(10); // exactly as many as actually existed
        assertThat(rejectedCount.get()).isEqualTo(10); // the rest correctly rejected, not overbooked

        Flight reloaded = flightRepository.findById(flightId).orElseThrow();
        assertThat(reloaded.getSeatsAvailable()).isZero(); // never negative, never left over
    }
}
