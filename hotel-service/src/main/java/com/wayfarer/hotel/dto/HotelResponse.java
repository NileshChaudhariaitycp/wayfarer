package com.wayfarer.hotel.dto;

import com.wayfarer.hotel.entity.Hotel;
import java.util.List;

public record HotelResponse(
        Long id,
        String name,
        String city,
        String address,
        int starRating,
        String description,
        List<RoomTypeResponse> roomTypes
) {
    public static HotelResponse from(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                hotel.getAddress(),
                hotel.getStarRating(),
                hotel.getDescription(),
                hotel.getRoomTypes().stream().map(RoomTypeResponse::from).toList());
    }
}
