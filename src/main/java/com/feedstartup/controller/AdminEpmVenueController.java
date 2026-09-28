package com.feedstartup.controller;

import com.feedstartup.dto.EpmVenueDto;
import com.feedstartup.dto.EpmVenueRequestDto;
import com.feedstartup.service.EpmVenueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The reusable EPM venue list. Gated behind the ADMIN role in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/epm/venues")
@CrossOrigin(origins = "*")
public class AdminEpmVenueController {

    private final EpmVenueService epmVenueService;

    @Autowired
    public AdminEpmVenueController(EpmVenueService epmVenueService) {
        this.epmVenueService = epmVenueService;
    }

    @GetMapping
    public ResponseEntity<List<EpmVenueDto>> list() {
        return ResponseEntity.ok(epmVenueService.list());
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<EpmVenueDto> create(@Valid @RequestBody EpmVenueRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(epmVenueService.create(dto));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<EpmVenueDto> update(@PathVariable Long id, @Valid @RequestBody EpmVenueRequestDto dto) {
        return ResponseEntity.ok(epmVenueService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        epmVenueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
