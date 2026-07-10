package com.wayfarer.auth.seed;

import com.wayfarer.auth.entity.Credential;
import com.wayfarer.auth.entity.Role;
import com.wayfarer.auth.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Guarded by a count check so re-running (e.g. app restart against the same
 * DB) never duplicates rows. H2 is in-memory here so it resets every restart
 * anyway, but the guard is what makes this seeder safe against a real
 * persistent DB too (Phase 7 swaps in Postgres) — write it idempotent from
 * day one rather than relying on "it happens to reset."
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "Password123!";

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (credentialRepository.count() > 0) {
            log.info("Credentials already seeded, skipping.");
            return;
        }

        seed("customer1", Role.CUSTOMER);
        seed("agent1", Role.TRAVEL_AGENT);
        seed("admin1", Role.ADMIN);

        log.info("=================================================");
        log.info(" Demo credentials (auth-service) — password for all: {}", DEMO_PASSWORD);
        log.info("   customer1 / {} -> CUSTOMER", DEMO_PASSWORD);
        log.info("   agent1    / {} -> TRAVEL_AGENT", DEMO_PASSWORD);
        log.info("   admin1    / {} -> ADMIN", DEMO_PASSWORD);
        log.info("=================================================");
    }

    private void seed(String username, Role role) {
        Credential credential = new Credential();
        credential.setUsername(username);
        credential.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        credential.setRole(role);
        credential.setEnabled(true);
        credentialRepository.save(credential);
    }
}
