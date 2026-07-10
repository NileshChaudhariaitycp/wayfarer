package com.wayfarer.loyalty.dto;

import com.wayfarer.loyalty.entity.LoyaltyAccount;

public record LoyaltyAccountResponse(
        Long userId,
        int pointsBalance
) {
    public static LoyaltyAccountResponse from(LoyaltyAccount account) {
        return new LoyaltyAccountResponse(account.getUserId(), account.getPointsBalance());
    }
}
