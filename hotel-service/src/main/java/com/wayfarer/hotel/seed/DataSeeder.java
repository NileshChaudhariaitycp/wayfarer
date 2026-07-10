package com.wayfarer.hotel.seed;

import com.wayfarer.hotel.entity.Hotel;
import com.wayfarer.hotel.entity.RoomType;
import com.wayfarer.hotel.repository.HotelRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private record HotelSeed(String name, String city, int starRating, String description) {
    }

    private static final HotelSeed[] HOTELS = {
            new HotelSeed("Wayfarer Grand", "New York", 5, "Flagship property overlooking Central Park."),
            new HotelSeed("Wayfarer Downtown", "New York", 4, "Business-district hotel near Wall Street."),
            new HotelSeed("Wayfarer Beachfront", "Los Angeles", 4, "Oceanview rooms steps from Santa Monica Pier."),
            new HotelSeed("Wayfarer Hills", "Los Angeles", 3, "Budget-friendly stay near Hollywood."),
            new HotelSeed("Wayfarer Riverside", "Chicago", 4, "Riverwalk views, walking distance to the Loop."),
            new HotelSeed("Wayfarer Bay", "San Francisco", 5, "Waterfront luxury near Fisherman's Wharf."),
            new HotelSeed("Wayfarer Peachtree", "Atlanta", 3, "Convenient midtown Atlanta location."),
            new HotelSeed("Wayfarer South Beach", "Miami", 4, "Art Deco district, steps from the beach."),
            new HotelSeed("Wayfarer Piccadilly", "London", 5, "Historic property in the heart of London."),
            new HotelSeed("Wayfarer Convention Center", "Dallas", 3, "Practical stay near the convention center."),
    };

    private final HotelRepository hotelRepository;

    public DataSeeder(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public void run(String... args) {
        if (hotelRepository.count() > 0) {
            log.info("Hotels already seeded, skipping.");
            return;
        }

        for (HotelSeed seed : HOTELS) {
            Hotel hotel = new Hotel();
            hotel.setName(seed.name());
            hotel.setCity(seed.city());
            hotel.setAddress("100 Main St, " + seed.city());
            hotel.setStarRating(seed.starRating());
            hotel.setDescription(seed.description());
            hotel.getRoomTypes().add(roomType(hotel, "Standard Queen", "89.00", 30));
            hotel.getRoomTypes().add(roomType(hotel, "Deluxe King", "149.00", 15));
            hotel.getRoomTypes().add(roomType(hotel, "Suite", "249.00", 5));
            hotelRepository.save(hotel);
        }

        log.info("Seeded {} demo hotels, each with 3 room types.", HOTELS.length);
    }

    private RoomType roomType(Hotel hotel, String name, String price, int totalRooms) {
        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setName(name);
        roomType.setPricePerNight(new BigDecimal(price));
        roomType.setTotalRooms(totalRooms);
        roomType.setRoomsAvailable(totalRooms);
        return roomType;
    }
}
