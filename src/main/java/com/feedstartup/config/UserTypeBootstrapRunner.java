package com.feedstartup.config;

import com.feedstartup.model.DataMigration;
import com.feedstartup.model.UserType;
import com.feedstartup.repository.DataMigrationRepository;
import com.feedstartup.repository.UserTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the {@code user_types} table with the types people can register as: the three the
 * registration form always offered (Individual, Institutional, International Buyer) followed by
 * Student, Business Collaborator, Guest, Government and Executive. Recorded in
 * {@code data_migrations} like the EPM seeds (see EpmDataBootstrapRunner), so it runs once per
 * database; a type already in the table is left as it is. Values already stored on users are
 * deliberately not adopted as types - the old form accepted free text, so they may be anything.
 */
@Component
public class UserTypeBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserTypeBootstrapRunner.class);

    private static final String SEED_ID = "user-types-seed-v1";

    static final List<String> DEFAULT_TYPES = List.of(
            "Individual", "Institutional", "International Buyer",
            "Student", "Business Collaborator", "Guest", "Government", "Executive");

    private final DataMigrationRepository migrationRepository;
    private final UserTypeRepository userTypeRepository;

    public UserTypeBootstrapRunner(DataMigrationRepository migrationRepository, UserTypeRepository userTypeRepository) {
        this.migrationRepository = migrationRepository;
        this.userTypeRepository = userTypeRepository;
    }

    @Override
    public void run(String... args) {
        if (migrationRepository.existsById(SEED_ID)) return;
        try {
            int added = 0;
            for (int i = 0; i < DEFAULT_TYPES.size(); i++) {
                String name = DEFAULT_TYPES.get(i);
                if (userTypeRepository.findByNameIgnoreCase(name).isEmpty()) {
                    userTypeRepository.save(new UserType(name, i));
                    added++;
                }
            }
            migrationRepository.save(new DataMigration(SEED_ID));
            log.info("Seeded {} user type(s).", added);
        } catch (RuntimeException e) {
            log.error("User type seed failed - it will be retried on the next startup", e);
        }
    }
}
