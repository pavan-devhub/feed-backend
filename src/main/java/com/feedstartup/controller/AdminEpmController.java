package com.feedstartup.controller;

import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmVolunteerDto;
import com.feedstartup.service.EpmRegistrationService;
import com.feedstartup.service.EpmVolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Read-only views of the submissions collected by EpmSubmissionController, for staff following up
 * with registrants/volunteers. Gated behind the same shared admin key as the other admin EPM
 * controllers.
 */
@RestController
@RequestMapping("/api/admin/epm")
@CrossOrigin(origins = "*")
public class AdminEpmController {

    private final EpmRegistrationService epmRegistrationService;
    private final EpmVolunteerService epmVolunteerService;

    @Value("${feedworld.admin.upload-key}")
    private String adminUploadKey;

    @Autowired
    public AdminEpmController(EpmRegistrationService epmRegistrationService, EpmVolunteerService epmVolunteerService) {
        this.epmRegistrationService = epmRegistrationService;
        this.epmVolunteerService = epmVolunteerService;
    }

    @GetMapping("/registrations")
    public ResponseEntity<?> listRegistrations(@RequestHeader("X-Admin-Key") String providedKey,
                                                @RequestParam(required = false) Long eventId) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        List<EpmRegistrationDto> registrations = epmRegistrationService.list(eventId);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/volunteers")
    public ResponseEntity<?> listVolunteers(@RequestHeader("X-Admin-Key") String providedKey,
                                             @RequestParam(required = false) Long eventId) {
        ResponseEntity<?> denied = checkAdminKey(providedKey);
        if (denied != null) return denied;

        List<EpmVolunteerDto> volunteers = epmVolunteerService.list(eventId);
        return ResponseEntity.ok(volunteers);
    }

    private ResponseEntity<?> checkAdminKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(adminUploadKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "error", "Invalid or missing X-Admin-Key header"));
        }
        return null;
    }
}
