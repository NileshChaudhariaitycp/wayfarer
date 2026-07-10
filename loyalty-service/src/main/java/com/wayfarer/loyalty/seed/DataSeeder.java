package com.wayfarer.loyalty.seed;

import com.wayfarer.loyalty.entity.LoyaltyAccount;
import com.wayfarer.loyalty.repository.LoyaltyAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * userId 1/2/3 assumes auth-service's H2 IDENTITY sequence assigned
 * customer1/agent1/admin1 those ids — same fragile coupling as
 * user-service's seeder (see its DataSeeder for the full explanation).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final int STARTING_BALANCE = 500;

    private final LoyaltyAccountRepository accountRepository;

    public DataSeeder(LoyaltyAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("Loyalty accounts already seeded, skipping.");
            return;
        }

        accountRepository.save(new LoyaltyAccount(1L, STARTING_BALANCE));
        accountRepository.save(new LoyaltyAccount(2L, STARTING_BALANCE));
        accountRepository.save(new LoyaltyAccount(3L, STARTING_BALANCE));

        log.info("Seeded 3 demo loyalty accounts with {} starting points each.", STARTING_BALANCE);
    }
}
