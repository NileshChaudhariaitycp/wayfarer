package com.wayfarer.loyalty.repository;

import com.wayfarer.loyalty.entity.LoyaltyTransaction;
import com.wayfarer.loyalty.entity.TransactionType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    Optional<LoyaltyTransaction> findByBookingIdAndType(Long bookingId, TransactionType type);
}
