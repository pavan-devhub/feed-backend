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
    public ResponseEntity<PublicationDetailDto> lookup(@RequestParam Integer year, @RequestParam Integer month) {
        return ResponseEntity.ok(publicationService.getByYearAndMonth(year, month));
    }

    @GetMapping("/latest")
    public ResponseEntity<PublicationDetailDto> latest() {
        return ResponseEntity.ok(publicationService.getLatest());
    }

    @GetMapping("/window")
    public ResponseEntity<List<PublicationSummaryDto>> window(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(defaultValue = "12") int count) {
        return ResponseEntity.ok(publicationService.getWindow(year, month, count));
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
     * The same bytes as {@link #streamPdf}, deliberately NOT advertised as application/pdf and
     * carrying no Content-Disposition or filename. The in-page viewer fetches this with its own
     * authenticated request and re-wraps the bytes as a PDF blob client-side.
     *
     * Why: a response typed application/pdf is capturable content. Download managers (IDM and
     * friends) and "always download PDFs" browser settings grab it out from under the page -
     * they cancel the page's own request, which surfaces in the viewer as "Failed to fetch",
     * and raise their own save dialog. That is a download the user never asked for, on what was
     * supposed to be a view. Bytes typed as something no capture list knows about stay inside
     * the page. Downloading is unaffected: /{id}/file?download=true still serves a correct
     * application/pdf attachment, and that click is the only way a file ever leaves the app.
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
