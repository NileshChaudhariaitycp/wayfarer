package com.wayfarer.hotel.service;

import com.wayfarer.hotel.dto.HotelRequest;
import com.wayfarer.hotel.dto.HotelResponse;
import com.wayfarer.hotel.dto.RoomTypeRequest;
import com.wayfarer.hotel.dto.RoomTypeResponse;
import com.wayfarer.hotel.entity.Hotel;
import com.wayfarer.hotel.entity.RoomType;
import com.wayfarer.hotel.exception.HotelNotFoundException;
import com.wayfarer.hotel.exception.InsufficientRoomsException;
import com.wayfarer.hotel.exception.RoomTypeNotFoundException;
import com.wayfarer.hotel.repository.HotelRepository;
import com.wayfarer.hotel.repository.RoomTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** See flight-service's FlightService for the full reasoning on caching search results — same pattern here. */
@Service
@RequiredArgsConstructor
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Cacheable(value = "hotelSearch", key = "#city")
    public List<HotelResponse> search(String city) {
        return hotelRepository.findByCityIgnoreCase(city).stream().map(HotelResponse::from).toList();
    }

    public HotelResponse getById(Long id) {
        return hotelRepository.findById(id).map(HotelResponse::from)
                .orElseThrow(() -> new HotelNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "hotelSearch", allEntries = true)
    public HotelResponse create(HotelRequest request) {
        Hotel hotel = new Hotel();
        applyRequest(hotel, request);
        hotel = hotelRepository.save(hotel);
        log.info("Created hotel {} in {}", hotel.getName(), hotel.getCity());
        return HotelResponse.from(hotel);
    }

    @Transactional
    @CacheEvict(value = "hotelSearch", allEntries = true)
    public HotelResponse update(Long id, HotelRequest request) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new HotelNotFoundException(id));
        applyRequest(hotel, request);
        hotel = hotelRepository.save(hotel);
        log.info("Updated hotel {}", hotel.getName());
        return HotelResponse.from(hotel);
    }

    @Transactional
    @CacheEvict(value = "hotelSearch", allEntries = true)
    public void delete(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw new HotelNotFoundException(id);
        }
        hotelRepository.deleteById(id);
        log.info("Deleted hotel id={}", id);
    }

    @Transactional
    @CacheEvict(value = "hotelSearch", allEntries = true)
    public RoomTypeResponse addRoomType(Long hotelId, RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new HotelNotFoundException(hotelId));
        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setName(request.name());
        roomType.setPricePerNight(request.pricePerNight());
        roomType.setTotalRooms(request.totalRooms());
        roomType.setRoomsAvailable(request.totalRooms());
        roomType = roomTypeRepository.save(roomType);
        log.info("Added room type {} to hotel {}", roomType.getName(), hotel.getName());
        return RoomTypeResponse.from(roomType);
    }

    @Transactional
    @CacheEvict(value = "hotelSearch", allEntries = true)
    public RoomTypeResponse updateRoomType(Long hotelId, Long roomTypeId, RoomTypeRequest request) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .filter(rt -> rt.getHotel().getId().equals(hotelId))
                .orElseThrow(() -> new RoomTypeNotFoundException(hotelId, roomTypeId));
        int roomsBooked = roomType.getTotalRooms() - roomType.getRoomsAvailable();
        roomType.setName(request.name());
        roomType.setPricePerNight(request.pricePerNight());
        roomType.setTotalRooms(request.totalRooms());
        roomType.setRoomsAvailable(Math.max(0, request.totalRooms() - roomsBooked));
        roomType = roomTypeRepository.save(roomType);
        return RoomTypeResponse.from(roomType);
    }

    @Transactional
    @CacheEvict(value = "hotelSearch", allEntries = true)
    public void deleteRoomType(Long hotelId, Long roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .filter(rt -> rt.getHotel().getId().equals(hotelId))
                .orElseThrow(() -> new RoomTypeNotFoundException(hotelId, roomTypeId));
        roomTypeRepository.delete(roomType);
    }

    // Same pessimistic-locked reserve/release pattern as flight-service's
    // seat inventory — see ADR 0005. Not idempotent against a duplicate
    // release call, for the same reason: booking-service is the only
    // caller and guarantees at-most-once compensation per reservation.
    @Transactional
    public void reserveRooms(Long roomTypeId, int rooms) {
        RoomType roomType = roomTypeRepository.findByIdForUpdate(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(null, roomTypeId));
        if (roomType.getRoomsAvailable() < rooms) {
            throw new InsufficientRoomsException(roomTypeId, rooms, roomType.getRoomsAvailable());
        }
        roomType.setRoomsAvailable(roomType.getRoomsAvailable() - rooms);
        roomTypeRepository.save(roomType);
        log.info("Reserved {} room(s) of type {} ({} remaining)", rooms, roomTypeId, roomType.getRoomsAvailable());
    }

    @Transactional
    public void releaseRooms(Long roomTypeId, int rooms) {
        RoomType roomType = roomTypeRepository.findByIdForUpdate(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(null, roomTypeId));
        roomType.setRoomsAvailable(Math.min(roomType.getTotalRooms(), roomType.getRoomsAvailable() + rooms));
        roomTypeRepository.save(roomType);
        log.info("Released {} room(s) of type {} ({} now available)", rooms, roomTypeId, roomType.getRoomsAvailable());
    }

    private void applyRequest(Hotel hotel, HotelRequest request) {
        hotel.setName(request.name());
        hotel.setCity(request.city());
        hotel.setAddress(request.address());
        hotel.setStarRating(request.starRating());
        hotel.setDescription(request.description());
    }
}
