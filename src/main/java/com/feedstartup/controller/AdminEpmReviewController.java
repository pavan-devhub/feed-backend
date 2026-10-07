package com.feedstartup.controller;

import com.feedstartup.dto.EpmReviewDto;
import com.feedstartup.dto.EpmReviewPageDto;
import com.feedstartup.dto.EpmReviewRequestDto;
import com.feedstartup.service.EpmReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Every review in display order, a page at a time ({@code page} 0-based, {@code size} capped at
     * Paging.MAX_PAGE_SIZE), with how many are published.
     */
    @GetMapping
    public ResponseEntity<EpmReviewPageDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(epmReviewService.pageAll(page, size));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<EpmReviewDto> create(@Valid @RequestBody EpmReviewRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(epmReviewService.create(dto));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<EpmReviewDto> update(@PathVariable Long id, @Valid @RequestBody EpmReviewRequestDto dto) {
        return ResponseEntity.ok(epmReviewService.update(id, dto));
    }

    /** {@code delta} -1 moves the review one place up the EPM page's slider, 1 one place down. */
    @PostMapping("/{id}/move")
    public ResponseEntity<Void> move(@PathVariable Long id, @RequestParam int delta) {
        epmReviewService.move(id, delta);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        epmReviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
