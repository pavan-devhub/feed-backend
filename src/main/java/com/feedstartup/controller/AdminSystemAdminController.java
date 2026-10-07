package com.feedstartup.controller;

import com.feedstartup.dto.SystemAdminDto;
import com.feedstartup.dto.SystemAdminRequestDto;
import com.feedstartup.service.SystemAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The admin panel's "System Admins" section: every admin account, adding one and removing one.
 * Any admin can add another (SecurityConfig's hasRole("ADMIN") on /api/admin/**). A username,
 * mobile number or email already in use comes back as 409 {error, fields} - see
 * AlreadyTakenException. Only the default admin can remove one (403 for anyone else), and the
 * default admin can't be removed (409).
 */
@RestController
@RequestMapping("/api/admin/system-admins")
@CrossOrigin(origins = "*")
public class AdminSystemAdminController {

    private final SystemAdminService systemAdminService;

    public AdminSystemAdminController(SystemAdminService systemAdminService) {
        this.systemAdminService = systemAdminService;
    }

    @GetMapping
    public ResponseEntity<List<SystemAdminDto>> list() {
        return ResponseEntity.ok(systemAdminService.list());
    }

    @PostMapping
    public ResponseEntity<SystemAdminDto> create(@Valid @RequestBody SystemAdminRequestDto dto, Authentication authentication) {
        // JwtAuthenticationFilter sets the logged-in admin's id as the credentials.
        Long adminId = (Long) authentication.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED).body(systemAdminService.create(dto, adminId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        systemAdminService.delete(id, (Long) authentication.getCredentials());
        return ResponseEntity.noContent().build();
    }
}
