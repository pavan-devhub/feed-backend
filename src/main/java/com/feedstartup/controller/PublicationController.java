package com.feedstartup.controller;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import com.feedstartup.model.PublicationLanguage;
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

    /**
     * Content type used for the viewer stream only. Intentionally not application/pdf - see
     * {@link #streamPdfForViewer}. The frontend re-labels the bytes as a PDF blob in the browser.
     */
    static final String VIEWER_STREAM_CONTENT_TYPE = "application/x-feedworld-publication";

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
    public ResponseEntity<PublicationDetailDto> lookup(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(defaultValue = "English") PublicationLanguage language) {
        return ResponseEntity.ok(publicationService.getByYearAndMonth(year, month, language));
    }

    @GetMapping("/latest")
    public ResponseEntity<PublicationDetailDto> latest(@RequestParam(defaultValue = "English") PublicationLanguage language) {
        return ResponseEntity.ok(publicationService.getLatest(language));
    }

    @GetMapping("/window")
    public ResponseEntity<List<PublicationSummaryDto>> window(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(defaultValue = "English") PublicationLanguage language,
            @RequestParam(defaultValue = "12") int count) {
        return ResponseEntity.ok(publicationService.getWindow(year, month, language, count));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicationDetailDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(publicationService.getById(id));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<org.springframework.core.io.Resource> streamPdf(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean download) {
        StoredFile file = publicationService.loadPdfFile(id);
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + file.filename() + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file.resource());
    }

    /**
     * Same bytes as {@link #streamPdf} but not advertised as application/pdf, so download
     * managers / "always download PDFs" settings don't intercept the viewer's own fetch. The
     * viewer re-wraps the bytes as a PDF blob client-side. Actual downloads still go through
     * /{id}/file?download=true with a proper application/pdf content type.
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<org.springframework.core.io.Resource> streamPdfForViewer(@PathVariable String id) {
        StoredFile file = publicationService.loadPdfFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(VIEWER_STREAM_CONTENT_TYPE))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(file.resource());
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<org.springframework.core.io.Resource> streamThumbnail(@PathVariable String id) {
        StoredFile file = publicationService.loadThumbnail(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(file.resource());
    }
}
