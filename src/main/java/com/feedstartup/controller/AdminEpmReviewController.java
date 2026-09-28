package com.feedstartup.controller;

import com.feedstartup.dto.EpmReviewDto;
import com.feedstartup.dto.EpmReviewRequestDto;
import com.feedstartup.service.EpmReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** EPM page testimonials, published or not. Gated behind the ADMIN role in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/epm/reviews")
@CrossOrigin(origins = "*")
public class AdminEpmReviewController {

    private final EpmReviewService epmReviewService;

    @Autowired
    public AdminEpmReviewController(EpmReviewService epmReviewService) {
        this.epmReviewService = epmReviewService;
    }

    @GetMapping
    public ResponseEntity<List<EpmReviewDto>> list() {
        return ResponseEntity.ok(epmReviewService.listAll());
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<EpmReviewDto> create(@Valid @RequestBody EpmReviewRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(epmReviewService.create(dto));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<EpmReviewDto> update(@PathVariable Long id, @Valid @RequestBody EpmReviewRequestDto dto) {
        return ResponseEntity.ok(epmReviewService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        epmReviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
