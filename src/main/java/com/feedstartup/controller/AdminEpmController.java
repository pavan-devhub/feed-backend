package com.feedstartup.controller;

import com.feedstartup.dto.EpmAdminOverviewDto;
import com.feedstartup.dto.EpmRegistrationDto;
import com.feedstartup.dto.EpmVolunteerDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.service.EpmAdminOverviewService;
import com.feedstartup.service.EpmRegistrationService;
import com.feedstartup.service.EpmSubmissionFilter;
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
 *
 * <p>The registration and volunteer lists come a page at a time; their /export twins return every
 * matching row for the spreadsheet download. All take the same filters:
 * {@code eventId}; {@code eventDate} - people signed up for an EPM held on this date (yyyy-MM-dd);
 * {@code submittedOn} - people who sent the form on this date (yyyy-MM-dd); {@code q} - free text.
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

    /** @param page 0-based page number; {@code size} is capped at EpmSubmissionFilter.MAX_PAGE_SIZE */
    @GetMapping("/registrations")
    public ResponseEntity<PageDto<EpmRegistrationDto>> listRegistrations(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedOn,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(epmRegistrationService.page(new EpmSubmissionFilter(eventId, eventDate, submittedOn, query), page, size));
    }

    @GetMapping("/registrations/export")
    public ResponseEntity<List<EpmRegistrationDto>> exportRegistrations(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedOn,
            @RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(epmRegistrationService.list(new EpmSubmissionFilter(eventId, eventDate, submittedOn, query)));
    }

    /** @param page 0-based page number; {@code size} is capped at EpmSubmissionFilter.MAX_PAGE_SIZE */
    @GetMapping("/volunteers")
    public ResponseEntity<PageDto<EpmVolunteerDto>> listVolunteers(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedOn,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(epmVolunteerService.page(new EpmSubmissionFilter(eventId, eventDate, submittedOn, query), page, size));
    }

    @GetMapping("/volunteers/export")
    public ResponseEntity<List<EpmVolunteerDto>> exportVolunteers(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedOn,
            @RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(epmVolunteerService.list(new EpmSubmissionFilter(eventId, eventDate, submittedOn, query)));
    }
}
