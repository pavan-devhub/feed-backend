package com.feedstartup.controller;

import com.feedstartup.dto.EpmCancelRequestDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventFacetsDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.dto.EpmLocationDto;
import com.feedstartup.dto.PageDto;
import com.feedstartup.service.EpmEventFilter;
import com.feedstartup.service.EpmEventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The EPM calendar - upcoming and previous meetings alike (whether an event is "upcoming" is just
 * whether its date is today or later). Gated behind the ADMIN role in SecurityConfig. Every change
 * is logged under the admin making it, for the EPM Activity log.
 */
@RestController
@RequestMapping("/api/admin/epm/events")
@CrossOrigin(origins = "*")
public class AdminEpmEventController {

    private final EpmEventService epmEventService;

    @Autowired
    public AdminEpmEventController(EpmEventService epmEventService) {
        this.epmEventService = epmEventService;
    }

    /**
     * Every matching EPM, cancelled ones included, each with its registration and volunteer counts -
     * the lookups behind the Registrations / Volunteers filters and the overview's next EPMs.
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
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(epmEventService.list(
                EpmEventFilter.forAdmin(status, query, state, district, city, category, month, year)));
    }

    /**
     * The EPM Events table: the same list a page at a time. {@code page} is 0-based and
     * {@code size} is capped at Paging.MAX_PAGE_SIZE.
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(epmEventService.page(
                EpmEventFilter.forAdmin(status, query, state, district, city, category, month, year), page, size));
    }

    /** The EPM Events filters' choices for one tab (cancelled EPMs included): where its EPMs are held. */
    @GetMapping("/facets")
    public ResponseEntity<EpmEventFacetsDto> facets(@RequestParam(defaultValue = "upcoming") String status) {
        return ResponseEntity.ok(epmEventService.facets(
                EpmEventFilter.forAdmin(status, null, null, null, null, null, null, null)));
    }

    /** Suggestions for the EPM form's state / district / place / venue fields. */
    @GetMapping("/locations")
    public ResponseEntity<List<EpmLocationDto>> locations() {
        return ResponseEntity.ok(epmEventService.listLocations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EpmEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(epmEventService.getById(id));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<EpmEventDto> create(@Valid @RequestBody EpmEventRequestDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(epmEventService.create(dto, adminId(authentication)));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<EpmEventDto> update(@PathVariable Long id, @Valid @RequestBody EpmEventRequestDto dto,
                                              Authentication authentication) {
        return ResponseEntity.ok(epmEventService.update(id, dto, adminId(authentication)));
    }

    /** Calls the EPM off: it leaves the public pages and shows as cancelled - with the optional reason - to everyone signed up. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<EpmEventDto> cancel(@PathVariable Long id, @Valid @RequestBody(required = false) EpmCancelRequestDto dto,
                                              Authentication authentication) {
        return ResponseEntity.ok(epmEventService.cancel(id, dto == null ? null : dto.reason(), adminId(authentication)));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<EpmEventDto> restore(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(epmEventService.restore(id, adminId(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        epmEventService.delete(id, adminId(authentication));
        return ResponseEntity.noContent().build();
    }

    // JwtAuthenticationFilter sets the logged-in admin's id as the credentials.
    private static Long adminId(Authentication authentication) {
        return (Long) authentication.getCredentials();
    }
}
