package com.feedstartup.config;

import com.feedstartup.model.DataMigration;
import com.feedstartup.model.SystemAdmin;
import com.feedstartup.model.User;
import com.feedstartup.repository.DataMigrationRepository;
import com.feedstartup.repository.SystemAdminRepository;
import com.feedstartup.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Sets up the {@code system_admins} table, once per database (recorded in data_migrations like the
 * other seeds):
 * <ol>
 *   <li>Creates the default admin when there is none - username {@code feedworld.admin.username},
 *       email {@code feedworld.admin.email}. Its mobile number is {@code feedworld.admin.mobile},
 *       or else that of the users-table account with that email (the admin before this table
 *       existed). Its password is {@code feedworld.admin.initial-password} if given (e.g.
 *       --feedworld.admin.initial-password=... on the first start), else that old account's
 *       password, else a random one written to the log once.</li>
 *   <li>Retires the old email-based admin: users-table accounts with the ADMIN role become USERs,
 *       logged out everywhere - their old tokens no longer open the admin panel.</li>
 * </ol>
 */
@Component
@Order(3)
public class SystemAdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemAdminBootstrapRunner.class);

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final DataMigrationRepository migrationRepository;
    private final SystemAdminRepository adminRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    @Value("${feedworld.admin.username:admin}")
    private String username;

    @Value("${feedworld.admin.email}")
    private String email;

    @Value("${feedworld.admin.mobile:}")
    private String mobileNumber;

    @Value("${feedworld.admin.initial-password:}")
    private String initialPassword;

    public SystemAdminBootstrapRunner(DataMigrationRepository migrationRepository, SystemAdminRepository adminRepository,
                                      UserRepository userRepository, JdbcTemplate jdbc) {
        this.migrationRepository = migrationRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        once("system-admins-seed-v1", this::seedDefaultAdmin);
        once("system-admins-retire-user-admins-v1", this::retireUserAdmins);
    }

    private void once(String id, Runnable step) {
        if (migrationRepository.existsById(id)) return;
        try {
            step.run();
            migrationRepository.save(new DataMigration(id));
        } catch (RuntimeException e) {
            log.error("System admin step {} failed - it will be retried on the next startup", id, e);
        }
    }

    private void seedDefaultAdmin() {
        if (adminRepository.count() > 0) return;

        String adminEmail = email.trim().toLowerCase(Locale.ROOT);
        User former = userRepository.findByEmail(adminEmail).orElse(null);
        String mobile = StringUtils.hasText(mobileNumber) ? mobileNumber.trim() : former == null ? null : former.getPhone();
        if (mobile == null) {
            throw new IllegalStateException("Set feedworld.admin.mobile to the default admin's 10-digit mobile number");
        }

        SystemAdmin admin = new SystemAdmin();
        admin.setUsername(username.trim());
        admin.setEmail(adminEmail);
        admin.setMobileNumber(mobile);
        String passwordSource;
        if (StringUtils.hasText(initialPassword)) {
            admin.setPassword(BCrypt.hashpw(initialPassword, BCrypt.gensalt(12)));
            passwordSource = "feedworld.admin.initial-password";
        } else if (former != null) {
            admin.setPassword(former.getPassword());
            passwordSource = "the existing " + adminEmail + " account";
        } else {
            String generated = randomPassword();
            admin.setPassword(BCrypt.hashpw(generated, BCrypt.gensalt(12)));
            passwordSource = "a generated password: " + generated;
        }
        adminRepository.save(admin);
        log.warn("Created the default system admin \"{}\" ({}) with the password from {}.", admin.getUsername(), adminEmail, passwordSource);
    }

    private void retireUserAdmins() {
        int loggedOut = jdbc.update("DELETE FROM user_sessions WHERE user_id IN (SELECT id FROM users WHERE role = 'ADMIN')");
        int demoted = jdbc.update("UPDATE users SET role = 'USER' WHERE role = 'ADMIN'");
        if (demoted > 0) {
            log.info("Moved {} users-table admin account(s) to the USER role and ended {} of their sessions - admins now log in as system admins.",
                    demoted, loggedOut);
        }
    }

    private static String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        while (password.length() < 14) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        // Meets the same "a letter and a number" rule as admins created from the panel.
        password.append(random.nextInt(10)).append((char) ('a' + random.nextInt(26)));
        return password.toString();
    }
}
