package com.wayfarer.hotel.repository;

import com.wayfarer.hotel.entity.RoomType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    // Same pessimistic-write pattern as flight-service's findByIdForUpdate —
    // see ADR 0005.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RoomType rt where rt.id = :id")
    Optional<RoomType> findByIdForUpdate(@Param("id") Long id);
}
