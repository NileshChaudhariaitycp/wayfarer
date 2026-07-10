package com.wayfarer.booking.controller;

import com.wayfarer.booking.dto.BookFlightRequest;
import com.wayfarer.booking.dto.BookHotelRequest;
import com.wayfarer.booking.dto.BookingResponse;
import com.wayfarer.booking.security.GatewayHeaderAuthenticationFilter;
import com.wayfarer.booking.service.BookingOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingOrchestrationService bookingOrchestrationService;

    @PostMapping("/flights")
    public ResponseEntity<BookingResponse> bookFlight(@Valid @RequestBody BookFlightRequest request, HttpServletRequest httpRequest) {
        BookingResponse response = bookingOrchestrationService.bookFlight(
                request, userId(httpRequest), canActOnBehalf(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/hotels")
    public ResponseEntity<BookingResponse> bookHotel(@Valid @RequestBody BookHotelRequest request, HttpServletRequest httpRequest) {
        BookingResponse response = bookingOrchestrationService.bookHotel(
                request, userId(httpRequest), canActOnBehalf(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, HttpServletRequest httpRequest) {
        return bookingOrchestrationService.cancel(id, userId(httpRequest), isAdmin(httpRequest));
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        return bookingOrchestrationService.getById(id, userId(httpRequest), isAdmin(httpRequest));
    }

    @GetMapping("/me")
    public List<BookingResponse> listMine(HttpServletRequest httpRequest) {
        return bookingOrchestrationService.listMine(userId(httpRequest));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> listAll() {
        return bookingOrchestrationService.listAll();
    }

    private Long userId(HttpServletRequest request) {
        return Long.valueOf(request.getHeader(GatewayHeaderAuthenticationFilter.USER_ID_HEADER));
    }

    private boolean isAdmin(HttpServletRequest request) {
        return roles(request).contains("ADMIN");
    }

    private boolean canActOnBehalf(HttpServletRequest request) {
        List<String> roles = roles(request);
        return roles.contains("TRAVEL_AGENT") || roles.contains("ADMIN");
    }

    private List<String> roles(HttpServletRequest request) {
        String header = request.getHeader(GatewayHeaderAuthenticationFilter.USER_ROLES_HEADER);
        return header == null ? List.of() : List.of(header.split(","));
    }
}
