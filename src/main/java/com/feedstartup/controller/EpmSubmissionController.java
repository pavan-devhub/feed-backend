package com.feedstartup.controller;

import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmRegistrationRequestDto;
import com.feedstartup.dto.EpmVolunteerDto;
import com.feedstartup.dto.EpmVolunteerRequestDto;
import com.feedstartup.dto.UserTypeDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.User;
import com.feedstartup.repository.UserRepository;
import com.feedstartup.service.EpmRegistrationService;
import com.feedstartup.service.EpmVolunteerService;
import com.feedstartup.service.UserTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public submission endpoints for the "Register for EPM" and "Become an EPM Volunteer" forms.
 * Mirrors RegistrationController's try/catch + {status, error} response shape rather than
 * GlobalExceptionHandler's plain shape, since both are public form submissions the frontend
 * reads the same way (see Register.jsx / the new epmApi.js).
 */
@RestController
@RequestMapping("/api/epm")
@CrossOrigin(origins = "*")
public class EpmSubmissionController {

    private final EpmRegistrationService epmRegistrationService;
    private final EpmVolunteerService epmVolunteerService;
    private final UserRepository userRepository;
    private final UserTypeService userTypeService;

    @Autowired
    public EpmSubmissionController(EpmRegistrationService epmRegistrationService,
                                    EpmVolunteerService epmVolunteerService,
                                    UserRepository userRepository,
                                    UserTypeService userTypeService) {
        this.epmRegistrationService = epmRegistrationService;
        this.epmVolunteerService = epmVolunteerService;
        this.userRepository = userRepository;
        this.userTypeService = userTypeService;
    }

    /**
     * The "Participant Type" choices of both forms - Institutional, Individual, Business
     * Collaborator, Student, Executive and Guest - from the user_types table.
     */
    @GetMapping("/participant-types")
    public ResponseEntity<List<UserTypeDto>> participantTypes() {
        return ResponseEntity.ok(userTypeService.listEpmParticipantTypes());
    }

    @PostMapping(value = "/registrations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody EpmRegistrationRequestDto dto) {
        try {
            EpmRegistrationDto saved = epmRegistrationService.register(dto, currentUserId());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Registration received successfully");
            response.put("registrationId", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred while submitting your registration");
        }
    }

    @PostMapping(value = "/volunteers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> volunteer(@Valid @RequestBody EpmVolunteerRequestDto dto) {
        try {
            EpmVolunteerDto saved = epmVolunteerService.volunteer(dto, currentUserId());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Volunteer application received successfully");
            response.put("volunteerId", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResourceNotFoundException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred while submitting your application");
        }
    }

    /** The logged-in account sending the form, or null - both forms also work signed out. */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof String email) || "anonymousUser".equals(email)) {
            return null;
        }
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
