package com.feedstartup.config;

import com.feedstartup.model.DataMigration;
import com.feedstartup.model.UserType;
import com.feedstartup.repository.DataMigrationRepository;
import com.feedstartup.repository.UserTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the {@code user_types} table with the types people can register as: the three the
 * registration form always offered (Individual, Institutional, International Buyer) followed by
 * Student, Business Collaborator, Guest, Government and Executive. Then marks the six the EPM
 * register and volunteer forms offer as "Participant Type" (UserType#epmOrder), in their dropdown
 * order. Each step is recorded in {@code data_migrations} like the EPM seeds (see
 * EpmDataBootstrapRunner), so it runs once per database; a type already in the table is left as it
 * is. Values already stored on users are deliberately not adopted as types - the old form accepted
 * free text, so they may be anything.
 *
 * <p>Runs first: UserLinksMigrationRunner points every EPM sign-up at one of these types.
 */
@Component
@Order(1)
public class UserTypeBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserTypeBootstrapRunner.class);

    static final List<String> DEFAULT_TYPES = List.of(
            "Individual", "Institutional", "International Buyer",
            "Student", "Business Collaborator", "Guest", "Government", "Executive");

    /** The EPM forms' "Participant Type" choices, in dropdown order. */
    public static final List<String> EPM_PARTICIPANT_TYPES = List.of(
            "Institutional", "Individual", "Business Collaborator", "Student", "Executive", "Guest");

    private final DataMigrationRepository migrationRepository;
    private final UserTypeRepository userTypeRepository;

    public UserTypeBootstrapRunner(DataMigrationRepository migrationRepository, UserTypeRepository userTypeRepository) {
        this.migrationRepository = migrationRepository;
        this.userTypeRepository = userTypeRepository;
    }

    @Override
    public void run(String... args) {
        once("user-types-seed-v1", this::seedDefaults);
        once("user-types-epm-participants-v1", this::markEpmParticipantTypes);
    }

    private void once(String id, Runnable step) {
        if (migrationRepository.existsById(id)) return;
        try {
            step.run();
            migrationRepository.save(new DataMigration(id));
        } catch (RuntimeException e) {
            log.error("User type step {} failed - it will be retried on the next startup", id, e);
        }
    }

    private void seedDefaults() {
        int added = 0;
        for (int i = 0; i < DEFAULT_TYPES.size(); i++) {
            String name = DEFAULT_TYPES.get(i);
            if (userTypeRepository.findByNameIgnoreCase(name).isEmpty()) {
                userTypeRepository.save(new UserType(name, i));
                added++;
            }
        }
        log.info("Seeded {} user type(s).", added);
    }

    // Adds any of the six that a database is missing (at the end of the account form's list), and
    // takes every other type off the EPM forms.
    private void markEpmParticipantTypes() {
        for (UserType type : userTypeRepository.findAll()) {
            int position = indexOfIgnoreCase(type.getName());
            type.setEpmOrder(position < 0 ? null : position);
            userTypeRepository.save(type);
        }
        int next = (int) userTypeRepository.count();
        for (int i = 0; i < EPM_PARTICIPANT_TYPES.size(); i++) {
            if (userTypeRepository.findByNameIgnoreCase(EPM_PARTICIPANT_TYPES.get(i)).isEmpty()) {
                UserType type = new UserType(EPM_PARTICIPANT_TYPES.get(i), next++);
                type.setEpmOrder(i);
                userTypeRepository.save(type);
            }
        }
        log.info("EPM participant types: {}", EPM_PARTICIPANT_TYPES);
    }

    private static int indexOfIgnoreCase(String name) {
        for (int i = 0; i < EPM_PARTICIPANT_TYPES.size(); i++) {
            if (EPM_PARTICIPANT_TYPES.get(i).equalsIgnoreCase(name)) return i;
        }
        return -1;
    }
}
