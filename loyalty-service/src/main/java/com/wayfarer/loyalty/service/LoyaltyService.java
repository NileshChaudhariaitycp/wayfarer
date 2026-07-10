package com.wayfarer.loyalty.service;

import com.wayfarer.loyalty.dto.EarnPointsRequest;
import com.wayfarer.loyalty.dto.LoyaltyAccountResponse;
import com.wayfarer.loyalty.entity.LoyaltyAccount;
import com.wayfarer.loyalty.entity.LoyaltyTransaction;
import com.wayfarer.loyalty.entity.TransactionType;
import com.wayfarer.loyalty.repository.LoyaltyAccountRepository;
import com.wayfarer.loyalty.repository.LoyaltyTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyService.class);

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;

    @Transactional
    public LoyaltyAccountResponse earn(Long userId, EarnPointsRequest request) {
        // Idempotent: if booking-service retries this call (e.g. after a
        // timeout it thinks failed), don't credit the same booking twice.
        if (transactionRepository.findByBookingIdAndType(request.bookingId(), TransactionType.EARNED).isPresent()) {
            log.info("Points already earned for booking {}, skipping.", request.bookingId());
            return LoyaltyAccountResponse.from(getOrCreateAccount(userId));
        }

        LoyaltyAccount account = getOrCreateAccount(userId);
        account.setPointsBalance(account.getPointsBalance() + request.points());
        accountRepository.save(account);

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setUserId(userId);
        transaction.setBookingId(request.bookingId());
        transaction.setPoints(request.points());
        transaction.setType(TransactionType.EARNED);
        transactionRepository.save(transaction);

        log.info("Credited {} points to user {} for booking {} (new balance {})",
                request.points(), userId, request.bookingId(), account.getPointsBalance());
        return LoyaltyAccountResponse.from(account);
    }

    @Transactional
    public LoyaltyAccountResponse reverse(Long userId, Long bookingId) {
        var earnedTransaction = transactionRepository.findByBookingIdAndType(bookingId, TransactionType.EARNED);
        if (earnedTransaction.isEmpty()
                || transactionRepository.findByBookingIdAndType(bookingId, TransactionType.REVERSED).isPresent()) {
            // Nothing to reverse, or already reversed — idempotent no-op.
            log.info("No pending earn to reverse for booking {}, skipping.", bookingId);
            return LoyaltyAccountResponse.from(getOrCreateAccount(userId));
        }

        int pointsToReverse = earnedTransaction.get().getPoints();
        LoyaltyAccount account = getOrCreateAccount(userId);
        account.setPointsBalance(Math.max(0, account.getPointsBalance() - pointsToReverse));
        accountRepository.save(account);

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setUserId(userId);
        transaction.setBookingId(bookingId);
        transaction.setPoints(-pointsToReverse);
        transaction.setType(TransactionType.REVERSED);
        transactionRepository.save(transaction);

        log.info("Reversed {} points from user {} for booking {} (new balance {})",
                pointsToReverse, userId, bookingId, account.getPointsBalance());
        return LoyaltyAccountResponse.from(account);
    }

    public LoyaltyAccountResponse getBalance(Long userId) {
        return LoyaltyAccountResponse.from(getOrCreateAccount(userId));
    }

    private LoyaltyAccount getOrCreateAccount(Long userId) {
        return accountRepository.findById(userId).orElseGet(() -> accountRepository.save(new LoyaltyAccount(userId, 0)));
    }
}
