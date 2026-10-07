package com.feedstartup.controller;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventFacetsDto;
import com.feedstartup.dto.EpmStatsDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.dto.UserNotificationsDto;
import com.feedstartup.service.EpmEventFilter;
import com.feedstartup.service.EpmEventService;
import com.feedstartup.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public, read-only endpoints backing the EPM page's "Upcoming EPMs" list, the details/filter
 * screen, the location picker on the register/volunteer forms, and the notification bell of a
 * visitor who isn't logged in. The directory and the location picker are paged here (/page), with
 * the directory's filter choices from /facets.
 */
@RestController
@RequestMapping("/api/epm/events")
@CrossOrigin(origins = "*")
public class EpmEventController {

    private final EpmEventService epmEventService;
    private final UserNotificationService userNotificationService;

    @Autowired
    public EpmEventController(EpmEventService epmEventService, UserNotificationService userNotificationService) {
        this.epmEventService = epmEventService;
        this.userNotificationService = userNotificationService;
    }

    /**
     * @param q free text over title, place, district, state, venue and category
     * @param includeCancelled also list upcoming EPMs that were cancelled (the register / volunteer pages mark them)
     */
    @GetMapping
    public ResponseEntity<List<EpmEventDto>> list(
            @RequestParam(defaultValue = "upcoming") String status,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "false") boolean includeCancelled) {
        return ResponseEntity.ok(epmEventService.list(
                EpmEventFilter.forPublic(status, query, state, district, city, category, month, year, includeCancelled)));
    }

    /**
     * The same list a page at a time - the EPM directory and the register / volunteer pages.
     * {@code page} is 0-based and {@code size} is capped at Paging.MAX_PAGE_SIZE.
     */
    @GetMapping("/page")
    public ResponseEntity<PageDto<EpmEventDto>> page(
            @RequestParam(defaultValue = "upcoming") String status,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(epmEventService.page(
                EpmEventFilter.forPublic(status, query, state, district, city, category, month, year, includeCancelled),
                page, size));
    }

    /** The directory's filter choices for one tab: where its EPMs are held, and how many each category has. */
    @GetMapping("/facets")
    public ResponseEntity<EpmEventFacetsDto> facets(@RequestParam(defaultValue = "upcoming") String status) {
        return ResponseEntity.ok(epmEventService.facets(
                EpmEventFilter.forPublic(status, null, null, null, null, null, null, null, false)));
    }

    @GetMapping("/stats")
    public ResponseEntity<EpmStatsDto> stats() {
        return ResponseEntity.ok(epmEventService.getStats());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<EpmCategoryDto>> categories() {
        return ResponseEntity.ok(epmEventService.listCategories());
    }

    /** New-EPM announcements (15 days before each EPM) for the bell of a visitor who isn't logged in. */
    @GetMapping("/announcements")
    public ResponseEntity<UserNotificationsDto> announcements() {
        return ResponseEntity.ok(userNotificationService.announcements());
    }

    // Constrained to digits so it doesn't swallow the /page, /facets, /stats, /categories and
    // /announcements routes above.
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<EpmEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(epmEventService.getById(id));
    }
}
