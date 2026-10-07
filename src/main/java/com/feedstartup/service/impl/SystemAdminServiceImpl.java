package com.feedstartup.service.impl;

import com.feedstartup.dto.SystemAdminDto;
import com.feedstartup.dto.SystemAdminRequestDto;
import com.feedstartup.exception.AlreadyTakenException;
import com.feedstartup.exception.ConflictException;
import com.feedstartup.exception.ForbiddenException;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.SystemAdmin;
import com.feedstartup.repository.SystemAdminRepository;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.SystemAdminService;
import com.feedstartup.service.SystemAdminSessionService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SystemAdminServiceImpl implements SystemAdminService {

    private final SystemAdminRepository adminRepository;
    private final UserRepository userRepository;
    private final SystemAdminSessionService sessionService;

    public SystemAdminServiceImpl(SystemAdminRepository adminRepository, UserRepository userRepository,
                                  SystemAdminSessionService sessionService) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    @Override
    public List<SystemAdminDto> list() {
        return adminRepository.findAllByOrderByCreatedAtAscIdAsc().stream().map(SystemAdminDto::from).toList();
    }

    @Override
    public SystemAdminDto create(SystemAdminRequestDto dto, Long createdByAdminId) {
        String username = dto.getUsername().trim();
        String mobileNumber = dto.getMobileNumber().trim();
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);

        // Every clash is reported at once, so the form can mark each field - but only ever as
        // "taken", never who has it.
        List<String> taken = new ArrayList<>();
        if (adminRepository.existsByUsernameIgnoreCase(username)) taken.add("username");
        if (adminRepository.existsByMobileNumber(mobileNumber) || userRepository.existsByPhone(mobileNumber)) taken.add("mobileNumber");
        if (adminRepository.existsByEmailIgnoreCase(email) || userRepository.existsByEmailIgnoreCase(email)) taken.add("email");
        if (!taken.isEmpty()) {
            throw new AlreadyTakenException(taken, takenMessage(taken));
        }

        SystemAdmin admin = new SystemAdmin();
        admin.setUsername(username);
        admin.setMobileNumber(mobileNumber);
        admin.setEmail(email);
        admin.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(12)));
        admin.setCreatedBy(get(createdByAdminId));
        try {
            return SystemAdminDto.from(adminRepository.saveAndFlush(admin));
        } catch (DataIntegrityViolationException e) {
            // Another admin saved the same details between the checks above and this insert -
            // the table's unique keys caught it.
            throw new AlreadyTakenException(List.of(), "That username, mobile number or email is already taken - please choose another.");
        }
    }

    @Override
    @Transactional
    public void delete(Long id, Long requestedByAdminId) {
        if (!isDefaultAdmin(get(requestedByAdminId))) {
            throw new ForbiddenException("Only the default admin can remove admins.");
        }
        SystemAdmin admin = get(id);
        if (isDefaultAdmin(admin)) {
            throw new ConflictException("The default admin can't be removed.");
        }

        // Whoever this admin added is credited to the admin who added them instead. Their
        // created_by must stay set: a null one is what marks the default admin.
        List<SystemAdmin> added = adminRepository.findByCreatedBy_Id(id);
        added.forEach(a -> a.setCreatedBy(admin.getCreatedBy()));
        adminRepository.saveAll(added);

        // Without a session row their JWT no longer opens anything (see JwtAuthenticationFilter).
        sessionService.deleteAllForAdmin(id);
        adminRepository.delete(admin);
    }

    @Override
    public Optional<SystemAdmin> authenticate(String login, String password) {
        String key = login == null ? "" : login.trim();
        if (key.isEmpty()) return Optional.empty();

        // A username never contains "@" and never starts with a digit (see SystemAdminRequestDto),
        // so which of the three was typed is plain to see.
        Optional<SystemAdmin> admin = key.contains("@") ? adminRepository.findByEmailIgnoreCase(key)
                : key.matches("\\d{10}") ? adminRepository.findByMobileNumber(key)
                : adminRepository.findByUsernameIgnoreCase(key);
        admin.ifPresent(a -> {
            if (password == null || !BCrypt.checkpw(password, a.getPassword())) {
                throw new IllegalArgumentException("Invalid password");
            }
        });
        return admin;
    }

    @Override
    public SystemAdmin get(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + id));
    }

    // The one SystemAdminBootstrapRunner seeded - every other admin was added by someone.
    private static boolean isDefaultAdmin(SystemAdmin admin) {
        return admin.getCreatedBy() == null;
    }

    /** "That username is already taken…", "That mobile number and email are already taken…". */
    static String takenMessage(List<String> fields) {
        List<String> labels = fields.stream().map(field -> switch (field) {
            case "mobileNumber" -> "mobile number";
            default -> field;
        }).toList();
        if (labels.size() == 1) {
            return "That " + labels.get(0) + " is already taken - please choose another.";
        }
        String joined = String.join(", ", labels.subList(0, labels.size() - 1)) + " and " + labels.get(labels.size() - 1);
        return "That " + joined + " are already taken - please choose others.";
    }
}
