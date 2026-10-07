package com.feedstartup.controller;

import com.feedstartup.dto.EpmMyActivitiesDto;
import com.feedstartup.dto.EpmSignUpDetailsDto;
import com.feedstartup.service.EpmActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * The logged-in user's "Status of Activities": their EPM registrations and volunteer sign-ups with
 * each EPM's latest status and updates - and their own details the register / volunteer forms are
 * filled in with. Requires a login (SecurityConfig's /api/users/me/** rule).
 */
@RestController
@RequestMapping("/api/users/me")
@CrossOrigin(origins = "*")
public class UserEpmActivityController {

    private final EpmActivityService epmActivityService;

    @Autowired
    public UserEpmActivityController(EpmActivityService epmActivityService) {
        this.epmActivityService = epmActivityService;
    }

    @GetMapping("/epm-activities")
    public ResponseEntity<EpmMyActivitiesDto> mine(Authentication authentication) {
        // JwtAuthenticationFilter sets the account's email as the principal.
        return ResponseEntity.ok(epmActivityService.forUser((String) authentication.getPrincipal()));
    }

    @GetMapping("/epm-details")
    public ResponseEntity<EpmSignUpDetailsDto> details(Authentication authentication) {
        return ResponseEntity.ok(epmActivityService.signUpDetails((String) authentication.getPrincipal()));
    }
}
