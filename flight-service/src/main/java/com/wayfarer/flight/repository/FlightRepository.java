package com.wayfarer.flight.repository;

import com.wayfarer.flight.entity.Flight;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCaseAndDepartureTimeBetween(
            String originAirportCode, String destinationAirportCode, LocalDateTime from, LocalDateTime to);

    List<Flight> findByOriginAirportCodeIgnoreCaseAndDestinationAirportCodeIgnoreCase(
            String originAirportCode, String destinationAirportCode);

    // SELECT ... FOR UPDATE: blocks any other transaction trying to read this
    // same row with a lock until this transaction commits/rolls back. Must be
    // called from within a @Transactional method — the lock is only held for
    // the life of that transaction.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Flight f where f.id = :id")
    Optional<Flight> findByIdForUpdate(@Param("id") Long id);
}
