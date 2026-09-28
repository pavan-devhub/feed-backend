package com.feedstartup.controller;

import com.feedstartup.dto.EpmAdminOverviewDto;
import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmVolunteerDto;
import com.feedstartup.service.EpmAdminOverviewService;
import com.feedstartup.service.EpmRegistrationService;
import com.feedstartup.service.EpmVolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * The admin panel's EPM overview numbers, and read-only views of the submissions collected by
 * EpmSubmissionController for staff following up with registrants/volunteers. Like every
 * /api/admin/** route, gated behind the ADMIN role in SecurityConfig - the same login that manages
 * Feed World publications.
 */
@RestController
@RequestMapping("/api/admin/epm")
@CrossOrigin(origins = "*")
public class AdminEpmController {

    private final EpmRegistrationService epmRegistrationService;
    private final EpmVolunteerService epmVolunteerService;
    private final EpmAdminOverviewService epmAdminOverviewService;

    @Autowired
    public AdminEpmController(EpmRegistrationService epmRegistrationService, EpmVolunteerService epmVolunteerService,
                              EpmAdminOverviewService epmAdminOverviewService) {
        this.epmRegistrationService = epmRegistrationService;
        this.epmVolunteerService = epmVolunteerService;
        this.epmAdminOverviewService = epmAdminOverviewService;
    }

    @GetMapping("/overview")
    public ResponseEntity<EpmAdminOverviewDto> overview() {
        return ResponseEntity.ok(epmAdminOverviewService.getOverview());
    }

    /**
     * @param eventDate  people registered for an EPM held on this date (yyyy-MM-dd)
     * @param submittedOn people who submitted the form on this date (yyyy-MM-dd)
     */
    @GetMapping("/registrations")
    public ResponseEntity<List<EpmRegistrationDto>> listRegistrations(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedOn,
            @RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(epmRegistrationService.list(eventId, eventDate, submittedOn, query));
    }

    @GetMapping("/volunteers")
    public ResponseEntity<List<EpmVolunteerDto>> listVolunteers(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedOn,
            @RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(epmVolunteerService.list(eventId, eventDate, submittedOn, query));
    }
}
