package com.wayfarer.hotel.dto;

import com.wayfarer.hotel.entity.RoomType;
import java.math.BigDecimal;

public record RoomTypeResponse(
        Long id,
        String name,
        BigDecimal pricePerNight,
        int totalRooms,
        int roomsAvailable
) {
    public static RoomTypeResponse from(RoomType roomType) {
        return new RoomTypeResponse(
                roomType.getId(),
                roomType.getName(),
                roomType.getPricePerNight(),
                roomType.getTotalRooms(),
                roomType.getRoomsAvailable());
    }
}
