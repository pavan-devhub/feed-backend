package com.feedstartup.controller;

import com.feedstartup.dto.EpmEventDto;
import com.feedstartup.dto.EpmEventRequestDto;
import com.feedstartup.service.EpmEventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The EPM calendar - upcoming and previous meetings alike (whether an event is "upcoming" is just
 * whether its date is today or later). Gated behind the ADMIN role in SecurityConfig.
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

    /** Includes cancelled events, each with its registration and volunteer counts. */
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
        return ResponseEntity.ok(epmEventService.adminList(status, query, state, district, city, category, month, year));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EpmEventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(epmEventService.getById(id));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<EpmEventDto> create(@Valid @RequestBody EpmEventRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(epmEventService.create(dto));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<EpmEventDto> update(@PathVariable Long id, @Valid @RequestBody EpmEventRequestDto dto) {
        return ResponseEntity.ok(epmEventService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        epmEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
