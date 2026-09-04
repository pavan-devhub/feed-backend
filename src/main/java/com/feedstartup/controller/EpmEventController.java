package com.feedstartup.controller;

import com.feedstartup.dto.EpmCategoryDto;
import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmStatsDto;
import com.feedstartup.service.EpmEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public, read-only endpoints backing the EPM page's "Upcoming EPMs" list, the details/filter
 * screen, and the location picker on the register/volunteer forms.
 */
@RestController
@RequestMapping("/api/epm/events")
@CrossOrigin(origins = "*")
public class EpmEventController {

    private final EpmEventService epmEventService;

    @Autowired
    public EpmEventController(EpmEventService epmEventService) {
        this.epmEventService = epmEventService;
    }

    @GetMapping
    public ResponseEntity<List<EpmEventDto>> list(
            @RequestParam(defaultValue = "upcoming") String status,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(epmEventService.list(status, state, district, city, category, month, year));
    }

    @GetMapping("/stats")
    public ResponseEntity<EpmStatsDto> stats() {
        return ResponseEntity.ok(epmEventService.getStats());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<EpmCategoryDto>> categories() {
        return ResponseEntity.ok(epmEventService.listCategories());
    }

    // Constrained to digits so it doesn't swallow the /stats and /categories routes above.
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<EpmEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(epmEventService.getById(id));
    }
}
