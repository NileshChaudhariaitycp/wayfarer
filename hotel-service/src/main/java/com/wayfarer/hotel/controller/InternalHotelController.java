package com.wayfarer.hotel.controller;

import com.wayfarer.hotel.dto.RoomsRequest;
import com.wayfarer.hotel.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only — called directly by booking-service, never routed through the gateway. */
@RestController
@RequestMapping("/internal/room-types")
@RequiredArgsConstructor
public class InternalHotelController {

    private final HotelService hotelService;

    @PostMapping("/{roomTypeId}/reserve")
    public void reserve(@PathVariable Long roomTypeId, @Valid @RequestBody RoomsRequest request) {
        hotelService.reserveRooms(roomTypeId, request.rooms());
    }

    @PostMapping("/{roomTypeId}/release")
    public void release(@PathVariable Long roomTypeId, @Valid @RequestBody RoomsRequest request) {
        hotelService.releaseRooms(roomTypeId, request.rooms());
    }
}
