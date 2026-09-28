package com.feedstartup.controller;

import com.feedstartup.dto.EpmReviewDto;
import com.feedstartup.service.EpmReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Public, read-only: the published testimonials for the EPM page's slider. */
@RestController
@RequestMapping("/api/epm/reviews")
@CrossOrigin(origins = "*")
public class EpmReviewController {

    private final EpmReviewService epmReviewService;

    @Autowired
    public EpmReviewController(EpmReviewService epmReviewService) {
        this.epmReviewService = epmReviewService;
    }

    @GetMapping
    public ResponseEntity<List<EpmReviewDto>> list() {
        return ResponseEntity.ok(epmReviewService.listPublished());
    }
}
