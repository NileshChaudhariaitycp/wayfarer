package com.wayfarer.hotel.service;

import com.wayfarer.hotel.dto.HotelRequest;
import com.wayfarer.hotel.dto.HotelResponse;
import com.wayfarer.hotel.dto.RoomTypeRequest;
import com.wayfarer.hotel.dto.RoomTypeResponse;
import com.wayfarer.hotel.entity.Hotel;
import com.wayfarer.hotel.entity.RoomType;
import com.wayfarer.hotel.exception.HotelNotFoundException;
import com.wayfarer.hotel.exception.RoomTypeNotFoundException;
import com.wayfarer.hotel.repository.HotelRepository;
import com.wayfarer.hotel.repository.RoomTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

    public List<HotelResponse> search(String city) {
        return hotelRepository.findByCityIgnoreCase(city).stream().map(HotelResponse::from).toList();
    }

    public HotelResponse getById(Long id) {
        return hotelRepository.findById(id).map(HotelResponse::from)
                .orElseThrow(() -> new HotelNotFoundException(id));
    }

    @Transactional
    public HotelResponse create(HotelRequest request) {
        Hotel hotel = new Hotel();
        applyRequest(hotel, request);
        hotel = hotelRepository.save(hotel);
        log.info("Created hotel {} in {}", hotel.getName(), hotel.getCity());
        return HotelResponse.from(hotel);
    }

    @Transactional
    public HotelResponse update(Long id, HotelRequest request) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new HotelNotFoundException(id));
        applyRequest(hotel, request);
        hotel = hotelRepository.save(hotel);
        log.info("Updated hotel {}", hotel.getName());
        return HotelResponse.from(hotel);
    }

    @Transactional
    public void delete(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw new HotelNotFoundException(id);
        }
        hotelRepository.deleteById(id);
        log.info("Deleted hotel id={}", id);
    }

    @Transactional
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
    public void deleteRoomType(Long hotelId, Long roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .filter(rt -> rt.getHotel().getId().equals(hotelId))
                .orElseThrow(() -> new RoomTypeNotFoundException(hotelId, roomTypeId));
        roomTypeRepository.delete(roomType);
    }

    private void applyRequest(Hotel hotel, HotelRequest request) {
        hotel.setName(request.name());
        hotel.setCity(request.city());
        hotel.setAddress(request.address());
        hotel.setStarRating(request.starRating());
        hotel.setDescription(request.description());
    }
}
