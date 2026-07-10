package com.wayfarer.booking.client.resilient;

import com.wayfarer.booking.client.HotelServiceClient;
import com.wayfarer.booking.exception.DownstreamCallException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

/** Same reasoning as ResilientFlightClient — see its javadoc. */
@Component
public class ResilientHotelClient {

    private final HotelServiceClient hotelServiceClient;

    public ResilientHotelClient(HotelServiceClient hotelServiceClient) {
        this.hotelServiceClient = hotelServiceClient;
    }

    @CircuitBreaker(name = "hotelService", fallbackMethod = "getHotelFallback")
    @Retry(name = "hotelService")
    public HotelServiceClient.HotelDetails getHotel(Long id) {
        return hotelServiceClient.getHotel(id);
    }

    @CircuitBreaker(name = "hotelService", fallbackMethod = "reserveRoomsFallback")
    public void reserveRooms(Long roomTypeId, int rooms) {
        hotelServiceClient.reserveRooms(roomTypeId, new HotelServiceClient.RoomsRequest(rooms));
    }

    @CircuitBreaker(name = "hotelService", fallbackMethod = "releaseRoomsFallback")
    public void releaseRooms(Long roomTypeId, int rooms) {
        hotelServiceClient.releaseRooms(roomTypeId, new HotelServiceClient.RoomsRequest(rooms));
    }

    @SuppressWarnings("unused")
    private HotelServiceClient.HotelDetails getHotelFallback(Long id, Throwable t) {
        throw new DownstreamCallException("hotel-service getHotel failed for hotelId=" + id, t);
    }

    @SuppressWarnings("unused")
    private void reserveRoomsFallback(Long roomTypeId, int rooms, Throwable t) {
        throw new DownstreamCallException("hotel-service reserveRooms failed for roomTypeId=" + roomTypeId, t);
    }

    @SuppressWarnings("unused")
    private void releaseRoomsFallback(Long roomTypeId, int rooms, Throwable t) {
        throw new DownstreamCallException("hotel-service releaseRooms failed for roomTypeId=" + roomTypeId, t);
    }
}
