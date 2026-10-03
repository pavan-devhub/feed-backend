package com.feedstartup.config;

import com.feedstartup.model.DataMigration;
import com.feedstartup.repository.DataMigrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Links the tables that belong to a person to the {@code users} table, and EPM sign-ups to the
 * {@code user_types} table they pick their participant type from - with real foreign keys, which
 * the schema didn't have. Each step is recorded in {@code data_migrations} and runs once per
 * database (a failed step is logged and retried on the next startup):
 * <ol>
 *   <li>Participant types: every EPM registration and volunteer row gets its participant_type_id
 *       (Hibernate adds the column and its foreign key). The forms used to offer other choices as
 *       plain text - Farmer, FPO... on the register form, "experience" on the volunteer form - so
 *       those are filed under the closest new type (LEGACY_*), anything else under Guest, and the
 *       old text is kept as legacy_participant_type / legacy_experience. The column then becomes
 *       NOT NULL.</li>
 *   <li>Users: EPM sign-ups made signed out are linked to the account with the same email, else the
 *       same mobile number (as Status of Activities already matched them); then each table's
 *       user_id gets a foreign key to users - ON DELETE SET NULL for EPM sign-ups, which outlive
 *       an account, and CASCADE for sessions, ERS results and notification state, which don't.
 *       Rows that point at an account that no longer exists are unlinked or removed first, or the
 *       key couldn't be added.</li>
 * </ol>
 * Runs after UserTypeBootstrapRunner, which marks the EPM participant types.
 */
@Component
@Order(2)
public class UserLinksMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserLinksMigrationRunner.class);

    /** The register form's old "Participant Type" choices -> the type they're filed under now. */
    static final Map<String, String> LEGACY_REGISTRATION_TYPES = Map.of(
            "farmer", "Individual",
            "fpo", "Institutional",
            "pacs", "Institutional",
            "shg", "Institutional",
            "msme", "Business Collaborator",
            "exporter", "Business Collaborator",
            "entrepreneur", "Business Collaborator");

    /** The volunteer form's old "Experience / Background" choices -> participant type. */
    static final Map<String, String> LEGACY_VOLUNTEER_TYPES = Map.of(
            "agriculture / farming", "Individual",
            "fpo / cooperative", "Institutional",
            "student", "Student",
            "business / msme", "Business Collaborator",
            "social / community work", "Individual",
            "government / institutional", "Institutional");

    /** Where an old value that matches nothing goes. */
    static final String FALLBACK_TYPE = "Guest";

    private record UserLink(String table, String constraint, String onDelete) {}

    private static final List<UserLink> USER_LINKS = List.of(
            new UserLink("epm_registrations", "fk_epm_registrations_user", "SET NULL"),
            new UserLink("epm_volunteers", "fk_epm_volunteers_user", "SET NULL"),
            new UserLink("user_sessions", "fk_user_sessions_user", "CASCADE"),
            new UserLink("ers_assessments", "fk_ers_assessments_user", "CASCADE"),
            new UserLink("user_notification_state", "fk_user_notification_state_user", "CASCADE"));

    private final DataMigrationRepository migrationRepository;
    private final JdbcTemplate jdbc;

    public UserLinksMigrationRunner(DataMigrationRepository migrationRepository, JdbcTemplate jdbc) {
        this.migrationRepository = migrationRepository;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        once("epm-participant-type-links-v1", this::linkParticipantTypes);
        once("user-foreign-keys-v1", this::linkUserTables);
    }

    private void once(String id, Runnable step) {
        if (migrationRepository.existsById(id)) return;
        try {
            step.run();
            migrationRepository.save(new DataMigration(id));
        } catch (RuntimeException e) {
            log.error("Database link step {} failed - it will be retried on the next startup", id, e);
        }
    }

    // --- step 1: participant types ------------------------------------------------------------

    private void linkParticipantTypes() {
        Map<String, Long> typeIds = new HashMap<>();
        jdbc.query("SELECT id, name FROM user_types WHERE epm_order IS NOT NULL",
                rs -> { typeIds.put(rs.getString("name").toLowerCase(Locale.ROOT), rs.getLong("id")); });
        if (!typeIds.containsKey(FALLBACK_TYPE.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("The EPM participant types aren't in user_types yet");
        }
        linkParticipantType("epm_registrations", "participant_type", "legacy_participant_type", LEGACY_REGISTRATION_TYPES, typeIds);
        linkParticipantType("epm_volunteers", "experience", "legacy_experience", LEGACY_VOLUNTEER_TYPES, typeIds);
    }

    private void linkParticipantType(String table, String oldColumn, String keptAs, Map<String, String> legacy,
                                     Map<String, Long> typeIds) {
        if (!tableExists(table)) return;
        int filled = 0;
        if (columnExists(table, oldColumn)) {
            for (Map<String, Object> row : jdbc.queryForList(
                    "SELECT id, " + oldColumn + " AS old_value FROM " + table + " WHERE participant_type_id IS NULL")) {
                String type = participantTypeFor((String) row.get("old_value"), legacy, typeIds.keySet());
                filled += jdbc.update("UPDATE " + table + " SET participant_type_id = ? WHERE id = ?",
                        typeIds.get(type), row.get("id"));
            }
            // Kept for reference, and nullable now that new rows don't fill it.
            jdbc.execute("ALTER TABLE " + table + " CHANGE " + oldColumn + " " + keptAs + " VARCHAR(255) NULL");
        }
        filled += jdbc.update("UPDATE " + table + " SET participant_type_id = ? WHERE participant_type_id IS NULL",
                typeIds.get(FALLBACK_TYPE.toLowerCase(Locale.ROOT)));
        if (columnNullable(table, "participant_type_id")) {
            jdbc.execute("ALTER TABLE " + table + " MODIFY participant_type_id BIGINT NOT NULL");
        }
        // Hibernate normally adds this one with the column; make sure of it either way.
        ensureForeignKey(table, "participant_type_id", "fk_" + table + "_participant_type", "user_types", null);
        log.info("Linked {} row(s) of {} to their participant type", filled, table);
    }

    /**
     * The participant type an old free-text value is filed under, as one of {@code types} (lower-case
     * names): itself if it already names one (e.g. "Student"), else its LEGACY_* mapping, else Guest.
     */
    static String participantTypeFor(String oldValue, Map<String, String> legacy, Set<String> types) {
        String key = oldValue == null ? "" : oldValue.trim().toLowerCase(Locale.ROOT);
        if (types.contains(key)) return key;
        String mapped = legacy.getOrDefault(key, FALLBACK_TYPE).toLowerCase(Locale.ROOT);
        return types.contains(mapped) ? mapped : FALLBACK_TYPE.toLowerCase(Locale.ROOT);
    }

    // --- step 2: users ------------------------------------------------------------------------

    private void linkUserTables() {
        for (String table : List.of("epm_registrations", "epm_volunteers")) {
            if (!tableExists(table)) continue;
            int unlinked = jdbc.update("UPDATE " + table + " SET user_id = NULL"
                    + " WHERE user_id IS NOT NULL AND user_id NOT IN (SELECT id FROM users)");
            int byEmail = jdbc.update("UPDATE " + table + " s JOIN users u ON LOWER(u.email) = LOWER(TRIM(s.email))"
                    + " SET s.user_id = u.id WHERE s.user_id IS NULL AND s.email IS NOT NULL AND TRIM(s.email) <> ''");
            int byMobile = jdbc.update("UPDATE " + table + " s JOIN users u ON u.phone = TRIM(s.mobile_number)"
                    + " SET s.user_id = u.id WHERE s.user_id IS NULL");
            log.info("{}: linked {} sign-up(s) to an account by email and {} by mobile number; {} pointed at a removed account",
                    table, byEmail, byMobile, unlinked);
        }
        for (UserLink link : USER_LINKS) {
            if (!tableExists(link.table())) continue;
            if ("CASCADE".equals(link.onDelete())) {
                int removed = jdbc.update("DELETE FROM " + link.table() + " WHERE user_id NOT IN (SELECT id FROM users)");
                if (removed > 0) log.info("{}: removed {} row(s) of accounts that no longer exist", link.table(), removed);
            }
            ensureForeignKey(link.table(), "user_id", link.constraint(), "users", link.onDelete());
        }
    }

    // --- schema helpers -------------------------------------------------------------------------

    private void ensureForeignKey(String table, String column, String name, String referenced, String onDelete) {
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? AND REFERENCED_TABLE_NAME = ?",
                Integer.class, table, column, referenced);
        if (existing != null && existing > 0) return;
        jdbc.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " FOREIGN KEY (" + column + ") REFERENCES "
                + referenced + " (id)" + (onDelete == null ? "" : " ON DELETE " + onDelete));
        log.info("Added foreign key {} ({}.{} -> {}.id)", name, table, column, referenced);
    }

    private boolean tableExists(String table) {
        return count("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?", table) > 0;
    }

    private boolean columnExists(String table, String column) {
        return count("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                + " AND TABLE_NAME = ? AND COLUMN_NAME = ?", table, column) > 0;
    }

    private boolean columnNullable(String table, String column) {
        return count("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                + " AND TABLE_NAME = ? AND COLUMN_NAME = ? AND IS_NULLABLE = 'YES'", table, column) > 0;
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
