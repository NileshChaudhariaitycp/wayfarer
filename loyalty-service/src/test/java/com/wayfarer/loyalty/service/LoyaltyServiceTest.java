package com.wayfarer.loyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wayfarer.loyalty.dto.EarnPointsRequest;
import com.wayfarer.loyalty.dto.LoyaltyAccountResponse;
import com.wayfarer.loyalty.entity.LoyaltyAccount;
import com.wayfarer.loyalty.entity.LoyaltyTransaction;
import com.wayfarer.loyalty.entity.TransactionType;
import com.wayfarer.loyalty.repository.LoyaltyAccountRepository;
import com.wayfarer.loyalty.repository.LoyaltyTransactionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the idempotency guarantee described in ADR 0005/0007:
 * booking-service may retry earn()/reverse() (e.g. after a timeout it
 * mistakenly thinks failed), and these must be safe to call twice for the
 * same bookingId without double-crediting or double-reversing points.
 */
@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long BOOKING_ID = 100L;

    @Mock private LoyaltyAccountRepository accountRepository;
    @Mock private LoyaltyTransactionRepository transactionRepository;

    private LoyaltyService loyaltyService;

    @BeforeEach
    void setUp() {
        loyaltyService = new LoyaltyService(accountRepository, transactionRepository);
    }

    @Test
    void earn_firstCall_createsAccountCreditsPointsAndRecordsTransaction() {
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.EARNED))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(accountRepository.save(any(LoyaltyAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoyaltyAccountResponse response = loyaltyService.earn(USER_ID, new EarnPointsRequest(BOOKING_ID, 289));

        assertThat(response.pointsBalance()).isEqualTo(289);
        verify(transactionRepository).save(any(LoyaltyTransaction.class));
    }

    @Test
    void earn_calledTwiceForSameBooking_creditsPointsOnlyOnce() {
        // First call: no existing EARNED transaction yet.
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.EARNED))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new LoyaltyAccount(USER_ID, 289))); // state after the first earn()
        when(accountRepository.save(any(LoyaltyAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loyaltyService.earn(USER_ID, new EarnPointsRequest(BOOKING_ID, 289));

        // Second call (a retry from booking-service): an EARNED transaction
        // for this bookingId now exists, so this must be a pure no-op.
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.EARNED))
                .thenReturn(Optional.of(new LoyaltyTransaction()));

        LoyaltyAccountResponse secondResponse = loyaltyService.earn(USER_ID, new EarnPointsRequest(BOOKING_ID, 289));

        assertThat(secondResponse.pointsBalance()).isEqualTo(289); // unchanged, NOT 578
        verify(transactionRepository, times(1)).save(any(LoyaltyTransaction.class)); // only the first call's
        // 2, not 1: the first (real) earn() call saves the account twice —
        // once inside getOrCreateAccount to persist the freshly-created
        // zero-balance account, once more after crediting the points. The
        // second (idempotent, skipped) call never reaches either save.
        verify(accountRepository, times(2)).save(any(LoyaltyAccount.class));
    }

    @Test
    void reverse_afterEarn_deductsPointsAndRecordsReversal() {
        LoyaltyTransaction earned = new LoyaltyTransaction();
        earned.setPoints(289);
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.EARNED))
                .thenReturn(Optional.of(earned));
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.REVERSED))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(new LoyaltyAccount(USER_ID, 289)));
        when(accountRepository.save(any(LoyaltyAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoyaltyAccountResponse response = loyaltyService.reverse(USER_ID, BOOKING_ID);

        assertThat(response.pointsBalance()).isEqualTo(0);
        verify(transactionRepository).save(any(LoyaltyTransaction.class));
    }

    @Test
    void reverse_calledTwiceForSameBooking_onlyDeductsPointsOnce() {
        LoyaltyTransaction earned = new LoyaltyTransaction();
        earned.setPoints(289);
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.EARNED))
                .thenReturn(Optional.of(earned));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(new LoyaltyAccount(USER_ID, 289)));
        when(accountRepository.save(any(LoyaltyAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // First reverse(): no REVERSED transaction exists yet.
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.REVERSED))
                .thenReturn(Optional.empty());
        loyaltyService.reverse(USER_ID, BOOKING_ID);

        // Second reverse() (a retry): a REVERSED transaction now exists —
        // must be a no-op, not a second deduction below zero.
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.REVERSED))
                .thenReturn(Optional.of(new LoyaltyTransaction()));
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(new LoyaltyAccount(USER_ID, 0)));

        LoyaltyAccountResponse secondResponse = loyaltyService.reverse(USER_ID, BOOKING_ID);

        assertThat(secondResponse.pointsBalance()).isEqualTo(0);
        verify(transactionRepository, times(1)).save(any(LoyaltyTransaction.class)); // only the first reverse's
    }

    @Test
    void reverse_withNothingEverEarned_isANoOp() {
        when(transactionRepository.findByBookingIdAndType(BOOKING_ID, TransactionType.EARNED))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(USER_ID)).thenReturn(Optional.of(new LoyaltyAccount(USER_ID, 0)));

        LoyaltyAccountResponse response = loyaltyService.reverse(USER_ID, BOOKING_ID);

        assertThat(response.pointsBalance()).isEqualTo(0);
        verify(transactionRepository, never()).save(any(LoyaltyTransaction.class));
        verify(accountRepository, never()).save(any(LoyaltyAccount.class));
    }
}
