package com.wayfarer.user.seed;

import com.wayfarer.user.dto.CreateProfileRequest;
import com.wayfarer.user.repository.UserProfileRepository;
import com.wayfarer.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Mirrors auth-service's DataSeeder by hand: userId 1/2/3 assumes
 * auth-service's H2 IDENTITY column assigned customer1/agent1/admin1 those
 * exact ids, in that order, on a fresh in-memory DB. That's a fragile,
 * order-dependent coupling between two services' independently-seeded data
 * — acceptable for demo seed data, but exactly the kind of implicit
 * cross-service assumption that an event-driven approach (Kafka, Phase 5)
 * removes: user-service would instead react to a "UserRegistered" event
 * carrying the real id, rather than both sides independently guessing it.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    @Override
    public void run(String... args) {
        if (userProfileRepository.count() > 0) {
            log.info("Profiles already seeded, skipping.");
            return;
        }

        userProfileService.createProfile(new CreateProfileRequest(1L, "customer1", "customer1@wayfarer.example", "Casey Customer", "CUSTOMER"));
        userProfileService.createProfile(new CreateProfileRequest(2L, "agent1", "agent1@wayfarer.example", "Alex Agent", "TRAVEL_AGENT"));
        userProfileService.createProfile(new CreateProfileRequest(3L, "admin1", "admin1@wayfarer.example", "Ada Admin", "ADMIN"));

        log.info("Seeded 3 demo profiles (customer1, agent1, admin1).");
    }
}
