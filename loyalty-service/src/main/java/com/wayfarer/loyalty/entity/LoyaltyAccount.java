package com.wayfarer.loyalty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** @Id is the same userId used across auth-service/user-service — see UserProfile for the pattern. */
@Entity
@Table(name = "loyalty_accounts")
@Getter
@Setter
@NoArgsConstructor
public class LoyaltyAccount {

    @Id
    private Long userId;

    @Column(nullable = false)
    private int pointsBalance;

    public LoyaltyAccount(Long userId, int pointsBalance) {
        this.userId = userId;
        this.pointsBalance = pointsBalance;
    }
}
