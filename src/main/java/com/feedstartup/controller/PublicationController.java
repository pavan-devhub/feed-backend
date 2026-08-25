package com.feedstartup.controller;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import com.feedstartup.service.PublicationService;
import com.feedstartup.service.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public, read-only endpoints that back the Feed World publications page. Every PDF and
 * thumbnail is streamed from disk through this controller - the browser never talks to the
 * storage folder directly, so where/how files are stored can change without breaking the UI.
 */
@RestController
@RequestMapping("/api/publications")
@CrossOrigin(origins = "*")
public class PublicationController {

    private final PublicationService publicationService;

    @Autowired
    public PublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @GetMapping("/years")
    public ResponseEntity<List<YearSummaryDto>> listYears() {
        return ResponseEntity.ok(publicationService.listYears());
    }

    @GetMapping
    public ResponseEntity<List<PublicationSummaryDto>> listByYear(@RequestParam Integer year) {
        return ResponseEntity.ok(publicationService.listByYear(year));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PublicationSummaryDto>> search(@RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(publicationService.search(q));
    }

    @GetMapping("/lookup")
    public ResponseEntity<PublicationDetailDto> lookup(@RequestParam Integer year, @RequestParam Integer month) {
        return ResponseEntity.ok(publicationService.getByYearAndMonth(year, month));
    }

    @GetMapping("/latest")
    public ResponseEntity<PublicationDetailDto> latest() {
        return ResponseEntity.ok(publicationService.getLatest());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicationDetailDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(publicationService.getById(id));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<org.springframework.core.io.Resource> streamPdf(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean download) {
        StoredFile file = publicationService.loadPdfFile(id);
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + file.filename() + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file.resource());
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<org.springframework.core.io.Resource> streamThumbnail(@PathVariable Long id) {
        StoredFile file = publicationService.loadThumbnail(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file.resource());
    }
}
