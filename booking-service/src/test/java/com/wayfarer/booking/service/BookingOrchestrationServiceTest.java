package com.wayfarer.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wayfarer.booking.client.FlightServiceClient;
import com.wayfarer.booking.client.PaymentServiceClient;
import com.wayfarer.booking.client.resilient.ResilientFlightClient;
import com.wayfarer.booking.client.resilient.ResilientHotelClient;
import com.wayfarer.booking.client.resilient.ResilientLoyaltyClient;
import com.wayfarer.booking.client.resilient.ResilientPaymentClient;
import com.wayfarer.booking.dto.BookFlightRequest;
import com.wayfarer.booking.dto.BookingResponse;
import com.wayfarer.booking.entity.Booking;
import com.wayfarer.booking.entity.BookingStatus;
import com.wayfarer.booking.event.BookingEventPublisher;
import com.wayfarer.booking.exception.BookingFailedException;
import com.wayfarer.booking.exception.DownstreamCallException;
import com.wayfarer.booking.repository.BookingRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the Saga orchestration logic itself (ADR 0005): every
 * downstream client is mocked, so these tests verify ONLY the
 * step-ordering and compensating-action wiring in
 * BookingOrchestrationService — not real HTTP calls, real persistence, or
 * real concurrency control (that's flight-service's pessimistic-lock
 * integration test's job).
 */
@ExtendWith(MockitoExtension.class)
class BookingOrchestrationServiceTest {

    private static final Long FLIGHT_ID = 1L;
    private static final Long CALLER_ID = 42L;
    private static final BigDecimal BASE_PRICE = new BigDecimal("289.99");

    @Mock private BookingRepository bookingRepository;
    @Mock private ResilientFlightClient flightClient;
    @Mock private ResilientHotelClient hotelClient;
    @Mock private ResilientPaymentClient paymentClient;
    @Mock private ResilientLoyaltyClient loyaltyClient;
    @Mock private BookingEventPublisher bookingEventPublisher;

    private BookingOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new BookingOrchestrationService(
                bookingRepository, flightClient, hotelClient, paymentClient, loyaltyClient, bookingEventPublisher);

        // lenient(): not every test reaches these calls (e.g. the
        // agent-without-customerId test throws during request validation,
        // before any repository or flight-service interaction), so Mockito's
        // strict-stubbing check would otherwise flag them as unused there.
        //
        // Simulates JPA's GenerationType.IDENTITY assigning an id on first
        // save() — the orchestrator reads booking.getId() immediately after
        // the initial save, before any real database is involved.
        lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            if (booking.getId() == null) {
                booking.setId(100L);
            }
            return booking;
        });

        lenient().when(flightClient.getFlight(FLIGHT_ID))
                .thenReturn(new FlightServiceClient.FlightDetails(FLIGHT_ID, "WF100", BASE_PRICE, 150));
    }

    private BookFlightRequest request(int seats) {
        return new BookFlightRequest(FLIGHT_ID, seats, null, "tok_visa_test");
    }

    @Test
    void bookFlight_happyPath_reservesSeatsAuthorizesPaymentEarnsPointsAndConfirms() {
        when(paymentClient.authorize(anyLong(), any(BigDecimal.class), any()))
                .thenReturn(new PaymentServiceClient.PaymentResult(9L, 100L, BASE_PRICE, "AUTHORIZED"));

        BookingResponse response = service.bookFlight(request(1), CALLER_ID, false);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED.name());
        assertThat(response.customerId()).isEqualTo(CALLER_ID);
        assertThat(response.paymentId()).isEqualTo(9L);

        verify(flightClient).reserveSeats(FLIGHT_ID, 1);
        verify(paymentClient).authorize(100L, BASE_PRICE, "tok_visa_test");
        verify(loyaltyClient).earn(CALLER_ID, 100L, 289); // 1 point per dollar, truncated
        verify(flightClient, never()).releaseSeats(any(), anyInt());
        verify(paymentClient, never()).refund(any());
    }

    @Test
    void bookFlight_seatReservationFails_failsBookingWithoutTouchingPaymentOrLoyalty() {
        doThrow(new DownstreamCallException("no seats available", null))
                .when(flightClient).reserveSeats(FLIGHT_ID, 1);

        assertThatThrownBy(() -> service.bookFlight(request(1), CALLER_ID, false))
                .isInstanceOf(BookingFailedException.class)
                .hasMessageContaining("Seat reservation failed");

        verify(paymentClient, never()).authorize(any(), any(), any());
        verify(loyaltyClient, never()).earn(any(), any(), anyInt());
        verify(bookingRepository, times(2)).save(any(Booking.class)); // PENDING, then FAILED
    }

    @Test
    void bookFlight_paymentFails_releasesSeatsAndFailsBooking() {
        when(paymentClient.authorize(anyLong(), any(BigDecimal.class), any()))
                .thenThrow(new DownstreamCallException("card declined", null));

        assertThatThrownBy(() -> service.bookFlight(request(2), CALLER_ID, false))
                .isInstanceOf(BookingFailedException.class)
                .hasMessageContaining("Payment failed");

        verify(flightClient).reserveSeats(FLIGHT_ID, 2);
        verify(flightClient).releaseSeats(FLIGHT_ID, 2); // compensating action
        verify(loyaltyClient, never()).earn(any(), any(), anyInt());
    }

    @Test
    void bookFlight_loyaltyFails_refundsPaymentAndReleasesSeats() {
        when(paymentClient.authorize(anyLong(), any(BigDecimal.class), any()))
                .thenReturn(new PaymentServiceClient.PaymentResult(9L, 100L, BASE_PRICE, "AUTHORIZED"));
        doThrow(new DownstreamCallException("loyalty-service unreachable", null))
                .when(loyaltyClient).earn(any(), any(), anyInt());

        assertThatThrownBy(() -> service.bookFlight(request(1), CALLER_ID, false))
                .isInstanceOf(BookingFailedException.class)
                .hasMessageContaining("Loyalty points failed");

        verify(paymentClient).refund(9L); // compensating action, in reverse order of the Saga steps
        verify(flightClient).releaseSeats(FLIGHT_ID, 1);
    }

    @Test
    void bookFlight_customerCannotActOnBehalf_alwaysBooksForThemselves() {
        // A CUSTOMER-role caller supplying someone else's customerId is
        // silently overridden to their own id — the request payload's
        // customerId field is only honored for TRAVEL_AGENT/ADMIN callers.
        when(paymentClient.authorize(anyLong(), any(BigDecimal.class), any()))
                .thenReturn(new PaymentServiceClient.PaymentResult(9L, 100L, BASE_PRICE, "AUTHORIZED"));
        BookFlightRequest spoofedRequest = new BookFlightRequest(FLIGHT_ID, 1, 999L, "tok_visa_test");

        BookingResponse response = service.bookFlight(spoofedRequest, CALLER_ID, false);

        assertThat(response.customerId()).isEqualTo(CALLER_ID);
        verify(loyaltyClient).earn(CALLER_ID, 100L, 289);
    }

    @Test
    void bookFlight_agentActingOnBehalfWithoutCustomerId_throwsBeforeAnyDownstreamCall() {
        BookFlightRequest requestMissingCustomerId = new BookFlightRequest(FLIGHT_ID, 1, null, "tok_visa_test");

        assertThatThrownBy(() -> service.bookFlight(requestMissingCustomerId, CALLER_ID, true))
                .isInstanceOf(BookingFailedException.class)
                .hasMessageContaining("customerId is required");

        verify(flightClient, never()).reserveSeats(any(), anyInt());
        verify(bookingRepository, never()).save(any());
    }
}
