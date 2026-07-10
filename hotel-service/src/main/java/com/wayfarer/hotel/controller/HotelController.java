package com.wayfarer.hotel.controller;

import com.wayfarer.hotel.dto.HotelRequest;
import com.wayfarer.hotel.dto.HotelResponse;
import com.wayfarer.hotel.dto.RoomTypeRequest;
import com.wayfarer.hotel.dto.RoomTypeResponse;
import com.wayfarer.hotel.service.HotelService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping("/search")
    public List<HotelResponse> search(@RequestParam String city) {
        return hotelService.search(city);
    }

    @GetMapping("/{id}")
    public HotelResponse getById(@PathVariable Long id) {
        return hotelService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelResponse> create(@Valid @RequestBody HotelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public HotelResponse update(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        return hotelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hotelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{hotelId}/room-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomTypeResponse> addRoomType(
            @PathVariable Long hotelId, @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.addRoomType(hotelId, request));
    }

    @PutMapping("/{hotelId}/room-types/{roomTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomTypeResponse updateRoomType(
            @PathVariable Long hotelId, @PathVariable Long roomTypeId, @Valid @RequestBody RoomTypeRequest request) {
        return hotelService.updateRoomType(hotelId, roomTypeId, request);
    }

    @DeleteMapping("/{hotelId}/room-types/{roomTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoomType(@PathVariable Long hotelId, @PathVariable Long roomTypeId) {
        hotelService.deleteRoomType(hotelId, roomTypeId);
        return ResponseEntity.noContent().build();
    }
}
