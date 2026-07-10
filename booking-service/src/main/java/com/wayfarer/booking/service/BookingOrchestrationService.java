package com.wayfarer.booking.service;

import com.wayfarer.booking.client.FlightServiceClient;
import com.wayfarer.booking.client.HotelServiceClient;
import com.wayfarer.booking.client.PaymentServiceClient;
import com.wayfarer.booking.client.resilient.ResilientFlightClient;
import com.wayfarer.booking.client.resilient.ResilientHotelClient;
import com.wayfarer.booking.client.resilient.ResilientLoyaltyClient;
import com.wayfarer.booking.client.resilient.ResilientPaymentClient;
import com.wayfarer.booking.dto.BookFlightRequest;
import com.wayfarer.booking.dto.BookHotelRequest;
import com.wayfarer.booking.dto.BookingResponse;
import com.wayfarer.booking.entity.Booking;
import com.wayfarer.booking.entity.BookingStatus;
import com.wayfarer.booking.entity.BookingType;
import com.wayfarer.booking.event.BookingEvent;
import com.wayfarer.booking.event.BookingEventPublisher;
import com.wayfarer.booking.exception.BookingFailedException;
import com.wayfarer.booking.exception.BookingNotFoundException;
import com.wayfarer.booking.exception.DownstreamCallException;
import com.wayfarer.booking.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * The Saga orchestrator — see ADR 0005 for the full reasoning, and ADR 0007
 * for the resilience patterns below. Deliberately NOT wrapped in a single
 * @Transactional: each Booking row mutation (PENDING -> CONFIRMED/FAILED)
 * commits immediately and independently. If this process crashed
 * mid-flight, wrapping everything in one local transaction would roll back
 * even the PENDING row on restart, silently losing the fact that a seat was
 * already reserved (or a card already charged) in a completely different
 * service's database. A durably-visible PENDING row is what lets an
 * operator (or a future reconciliation job) notice and fix a stuck booking.
 *
 * All downstream calls go through Resilient*Client wrappers (circuit
 * breaker everywhere, automatic retry ONLY on the genuinely idempotent
 * operations) and surface failures as one consistent DownstreamCallException
 * instead of a library-specific exception type — see ADR 0007.
 *
 * Known gap, not fixed here: if a compensating call itself fails (e.g.
 * releaseSeats during payment-failure handling, after the circuit breaker
 * has already given up), that exception propagates uncaught out of this
 * service as a 500 rather than being retried or queued for later. A real
 * production system would route a failed compensation to a dead-letter
 * queue / manual-intervention alert rather than let it surface as a plain
 * request failure — deliberately out of scope for this project.
 */
@Service
@RequiredArgsConstructor
public class BookingOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(BookingOrchestrationService.class);

    private final BookingRepository bookingRepository;
    private final ResilientFlightClient flightClient;
    private final ResilientHotelClient hotelClient;
    private final ResilientPaymentClient paymentClient;
    private final ResilientLoyaltyClient loyaltyClient;
    private final BookingEventPublisher bookingEventPublisher;

    public BookingResponse bookFlight(BookFlightRequest request, Long callerId, boolean callerCanActOnBehalf) {
        Long customerId = resolveCustomerId(request.customerId(), callerId, callerCanActOnBehalf);

        // No Booking row exists yet at this point, so a failure here has
        // nothing to mark FAILED or compensate — just a clean rejection.
        FlightServiceClient.FlightDetails flight;
        try {
            flight = flightClient.getFlight(request.flightId());
        } catch (DownstreamCallException ex) {
            throw new BookingFailedException("Could not look up flight " + request.flightId() + " (" + ex.getMessage() + ")");
        }
        BigDecimal totalPrice = flight.basePrice().multiply(BigDecimal.valueOf(request.seats()));

        Booking booking = new Booking();
        booking.setBookingType(BookingType.FLIGHT);
        booking.setCustomerId(customerId);
        booking.setBookedByUserId(callerId);
        booking.setFlightId(request.flightId());
        booking.setQuantity(request.seats());
        booking.setUnitPrice(flight.basePrice());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);
        booking = bookingRepository.save(booking);
        Long bookingId = booking.getId();
        log.info("Booking {} PENDING: flight {} x{} seats for customer {}",
                bookingId, request.flightId(), request.seats(), customerId);

        // Step 1: reserve seats (pessimistic-locked in flight-service)
        try {
            flightClient.reserveSeats(request.flightId(), request.seats());
        } catch (DownstreamCallException ex) {
            return fail(booking, "Seat reservation failed (" + ex.getMessage() + ")");
        }

        // Step 2: authorize payment
        PaymentServiceClient.PaymentResult payment;
        try {
            payment = paymentClient.authorize(bookingId, totalPrice, request.cardToken());
        } catch (DownstreamCallException ex) {
            flightClient.releaseSeats(request.flightId(), request.seats());
            return fail(booking, "Payment failed (" + ex.getMessage() + ")");
        }
        booking.setPaymentId(payment.id());

        // Step 3: award loyalty points
        try {
            loyaltyClient.earn(customerId, bookingId, pointsFor(totalPrice));
        } catch (DownstreamCallException ex) {
            paymentClient.refund(payment.id());
            flightClient.releaseSeats(request.flightId(), request.seats());
            return fail(booking, "Loyalty points failed (" + ex.getMessage() + ")");
        }

        return confirm(booking);
    }

    public BookingResponse bookHotel(BookHotelRequest request, Long callerId, boolean callerCanActOnBehalf) {
        Long customerId = resolveCustomerId(request.customerId(), callerId, callerCanActOnBehalf);

        // No Booking row exists yet at this point — same reasoning as
        // bookFlight's initial getFlight() lookup.
        HotelServiceClient.HotelDetails hotel;
        try {
            hotel = hotelClient.getHotel(request.hotelId());
        } catch (DownstreamCallException ex) {
            throw new BookingFailedException("Could not look up hotel " + request.hotelId() + " (" + ex.getMessage() + ")");
        }
        HotelServiceClient.RoomTypeDetails roomType = hotel.roomTypes().stream()
                .filter(rt -> rt.id().equals(request.roomTypeId()))
                .findFirst()
                .orElseThrow(() -> new BookingFailedException(
                        "Room type " + request.roomTypeId() + " not found at hotel " + request.hotelId()));

        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        if (nights < 1) {
            throw new BookingFailedException("checkOutDate must be after checkInDate");
        }
        BigDecimal totalPrice = roomType.pricePerNight()
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(request.rooms()));

        Booking booking = new Booking();
        booking.setBookingType(BookingType.HOTEL);
        booking.setCustomerId(customerId);
        booking.setBookedByUserId(callerId);
        booking.setHotelId(request.hotelId());
        booking.setRoomTypeId(request.roomTypeId());
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setQuantity(request.rooms());
        booking.setUnitPrice(roomType.pricePerNight());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);
        booking = bookingRepository.save(booking);
        Long bookingId = booking.getId();
        log.info("Booking {} PENDING: hotel {} room type {} x{} rooms for customer {}",
                bookingId, request.hotelId(), request.roomTypeId(), request.rooms(), customerId);

        // Step 1: reserve rooms (pessimistic-locked in hotel-service)
        try {
            hotelClient.reserveRooms(request.roomTypeId(), request.rooms());
        } catch (DownstreamCallException ex) {
            return fail(booking, "Room reservation failed (" + ex.getMessage() + ")");
        }

        // Step 2: authorize payment
        PaymentServiceClient.PaymentResult payment;
        try {
            payment = paymentClient.authorize(bookingId, totalPrice, request.cardToken());
        } catch (DownstreamCallException ex) {
            hotelClient.releaseRooms(request.roomTypeId(), request.rooms());
            return fail(booking, "Payment failed (" + ex.getMessage() + ")");
        }
        booking.setPaymentId(payment.id());

        // Step 3: award loyalty points
        try {
            loyaltyClient.earn(customerId, bookingId, pointsFor(totalPrice));
        } catch (DownstreamCallException ex) {
            paymentClient.refund(payment.id());
            hotelClient.releaseRooms(request.roomTypeId(), request.rooms());
            return fail(booking, "Loyalty points failed (" + ex.getMessage() + ")");
        }

        return confirm(booking);
    }

    /** Reuses the exact same compensating calls as Saga failure handling — cancellation is "run the rollback, on purpose, later." */
    public BookingResponse cancel(Long bookingId, Long callerId, boolean callerIsAdmin) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException(bookingId));
        assertCanAccess(booking, callerId, callerIsAdmin);

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingFailedException("Booking " + bookingId + " is already " + booking.getStatus());
        }

        if (booking.getPaymentId() != null) {
            paymentClient.refund(booking.getPaymentId());
        }
        loyaltyClient.reverse(booking.getCustomerId(), bookingId);
        if (booking.getBookingType() == BookingType.FLIGHT) {
            flightClient.releaseSeats(booking.getFlightId(), booking.getQuantity());
        } else {
            hotelClient.releaseRooms(booking.getRoomTypeId(), booking.getQuantity());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        log.info("Booking {} CANCELLED", bookingId);
        bookingEventPublisher.publish(BookingEvent.cancelled(
                booking.getId(), booking.getCustomerId(), booking.getBookingType().name()));
        return BookingResponse.from(booking);
    }

    public BookingResponse getById(Long bookingId, Long callerId, boolean callerIsAdmin) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException(bookingId));
        assertCanAccess(booking, callerId, callerIsAdmin);
        return BookingResponse.from(booking);
    }

    public List<BookingResponse> listMine(Long callerId) {
        return bookingRepository.findByCustomerId(callerId).stream().map(BookingResponse::from).toList();
    }

    public List<BookingResponse> listAll() {
        return bookingRepository.findAll().stream().map(BookingResponse::from).toList();
    }

    private void assertCanAccess(Booking booking, Long callerId, boolean callerIsAdmin) {
        if (!callerIsAdmin && !booking.getCustomerId().equals(callerId) && !booking.getBookedByUserId().equals(callerId)) {
            throw new AccessDeniedException("Not your booking");
        }
    }

    private BookingResponse confirm(Booking booking) {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        log.info("Booking {} CONFIRMED, total {}", booking.getId(), booking.getTotalPrice());
        bookingEventPublisher.publish(BookingEvent.confirmed(
                booking.getId(), booking.getCustomerId(), booking.getBookingType().name(), booking.getTotalPrice()));
        return BookingResponse.from(booking);
    }

    private BookingResponse fail(Booking booking, String reason) {
        booking.setStatus(BookingStatus.FAILED);
        booking.setFailureReason(reason);
        booking = bookingRepository.save(booking);
        log.warn("Booking {} FAILED: {}", booking.getId(), reason);
        bookingEventPublisher.publish(BookingEvent.failed(
                booking.getId(), booking.getCustomerId(), booking.getBookingType().name(), reason));
        throw new BookingFailedException(reason);
    }

    private Long resolveCustomerId(Long requestedCustomerId, Long callerId, boolean callerCanActOnBehalf) {
        if (!callerCanActOnBehalf) {
            // CUSTOMER: always themselves — never trust a client-supplied
            // customerId for who "self" is.
            return callerId;
        }
        if (requestedCustomerId == null) {
            throw new BookingFailedException("customerId is required when booking on behalf of someone else");
        }
        return requestedCustomerId;
    }

    private int pointsFor(BigDecimal totalPrice) {
        return totalPrice.intValue(); // 1 point per dollar spent — simple, not yet configurable
    }
}
