package com.wayfarer.loyalty.repository;

import com.wayfarer.loyalty.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
}
