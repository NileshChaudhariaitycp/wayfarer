package com.wayfarer.booking.client;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/hotels/{id}")
    HotelDetails getHotel(@PathVariable("id") Long id);

    @PostMapping("/internal/room-types/{roomTypeId}/reserve")
    void reserveRooms(@PathVariable("roomTypeId") Long roomTypeId, @RequestBody RoomsRequest request);

    @PostMapping("/internal/room-types/{roomTypeId}/release")
    void releaseRooms(@PathVariable("roomTypeId") Long roomTypeId, @RequestBody RoomsRequest request);

    record RoomsRequest(int rooms) {
    }

    record HotelDetails(Long id, String name, List<RoomTypeDetails> roomTypes) {
    }

    record RoomTypeDetails(Long id, String name, BigDecimal pricePerNight, int roomsAvailable) {
    }
}
