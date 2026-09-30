package com.feedstartup.service.impl;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.Publication;
import com.feedstartup.model.PublicationLanguage;
import com.feedstartup.repository.PublicationRepository;
import com.feedstartup.service.PdfProcessingService;
import com.feedstartup.service.PublicationService;
import com.feedstartup.service.PublicationVisibility;
import com.feedstartup.service.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.feedstartup.repository.PublicationRepository.CALENDAR_ORDER;
import static com.feedstartup.repository.PublicationRepository.CATALOG_ORDER;

/**
 * The publication catalog is the {@code publications} table (see {@link Publication} /
 * {@link PublicationRepository}) - listing, search and lookups all query it directly, sorted by
 * the database via {@link PublicationRepository#CATALOG_ORDER}. Every month has up to three rows,
 * one per {@link PublicationLanguage} - Telugu, Hindi and English are separate PDFs, not one PDF
 * with three translations. Each row's PDF and cover thumbnail are plain files on disk under
 * {@code storage/publications/<year>/<month>/} (see {@code feedworld.storage.base-dir} and
 * {@link #buildPublicationDir}), a folder shared by that month's three editions; the row stores
 * their filenames, resolved against that folder whenever the PDF or thumbnail is actually
 * fetched. Filenames are {@code feed_world_<language>.pdf} / {@code .png} (see
 * {@link PublicationLanguage#storedFileBaseName}), so editions sharing a folder never collide, and
 * Replace/Delete only ever touch the files named on that one row. An issue's public id is its
 * "{year}-{month}-{language}" path (e.g. "2025-08-English"). Publications written under the
 * earlier {@code <year>/<language>/<month>/} layout or random filenames are moved and renamed
 * into place by {@link com.feedstartup.config.PublicationMigrationRunner} on startup.
 */
@Service
public class PublicationServiceImpl implements PublicationService {

    // The admin can upload an issue for any year. The only check is that it's a 4-digit year,
    // since it becomes the storage/publications/<year>/ folder name (and "Month Year" search
    // only recognises 4-digit years).
    private static final int MIN_YEAR = 1000;
    private static final int MAX_YEAR = 9999;

    private final PdfProcessingService pdfProcessingService;
    private final PublicationRepository publicationRepository;
    private final PublicationVisibility visibility;

    @Value("${feedworld.storage.base-dir}")
    private String baseDir;

    @Autowired
    public PublicationServiceImpl(PdfProcessingService pdfProcessingService, PublicationRepository publicationRepository,
                                  PublicationVisibility visibility) {
        this.pdfProcessingService = pdfProcessingService;
        this.publicationRepository = publicationRepository;
        this.visibility = visibility;
    }

    @Override
    public List<YearSummaryDto> listYears() {
        // CATALOG_ORDER puts the years newest first and each year's rows in language order, so
        // the grouping below keeps that order (LinkedHashMap); EnumMap iterates languages in
        // declaration order, which is the same Telugu, Hindi, English sequence. Unreleased issues
        // are dropped first, so for a normal user a future year - all of whose months are still
        // to come - isn't listed at all.
        Map<Integer, List<Publication>> byYear = publicationRepository.findAll(CATALOG_ORDER).stream()
                .filter(visibility.viewableByCaller())
                .collect(Collectors.groupingBy(Publication::getYear, LinkedHashMap::new, Collectors.toList()));
        return byYear.entrySet().stream()
                .map(e -> new YearSummaryDto(e.getKey(), (long) e.getValue().size(), readableLanguages(e.getValue())))
                .collect(Collectors.toList());
    }

    /** Per-language count of a year's editions whose PDF is on disk - see YearSummaryDto. */
    private List<YearSummaryDto.LanguageCount> readableLanguages(List<Publication> yearRows) {
        Map<PublicationLanguage, Long> counts = yearRows.stream()
                .filter(this::pdfExists)
                .collect(Collectors.groupingBy(Publication::getLanguage,
                        () -> new EnumMap<>(PublicationLanguage.class), Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> new YearSummaryDto.LanguageCount(e.getKey().name(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicationSummaryDto> listByYear(Integer year, PublicationLanguage language) {
        List<Publication> rows = language == null
                ? publicationRepository.findByYear(year, CATALOG_ORDER)
                : publicationRepository.findByYearAndLanguage(year, language, CALENDAR_ORDER);
        return toViewableSummaries(rows);
    }

    @Override
    public List<PublicationSummaryDto> search(String query) {
        if (query == null || query.isBlank()) {
            return toViewableSummaries(publicationRepository.findAll(CATALOG_ORDER));
        }

        // The archive search box is meant to be driven by "Month Year" (e.g. "August 2025", also
        // accepting the abbreviated month name and either token order) so a reader can jump
        // straight to one issue rather than hunting for it by title. Only a query where both a
        // month and a year were recognised takes this path; anything else - including a bare
        // year or a bare month name - falls through to the plain title search below. A month/year
        // match can return up to three rows (one per language).
        int[] monthYear = parseMonthYear(query);
        if (monthYear != null) {
            return toViewableSummaries(publicationRepository.findByYearAndMonth(monthYear[0], monthYear[1], CATALOG_ORDER));
        }

        return toViewableSummaries(publicationRepository.findByTitleContainingIgnoreCase(query, CATALOG_ORDER));
    }

    private List<PublicationSummaryDto> toViewableSummaries(List<Publication> rows) {
        return rows.stream()
                .filter(visibility.viewableByCaller())
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private static final Map<String, Integer> MONTH_NAME_TO_NUMBER = buildMonthNameMap();

    private static Map<String, Integer> buildMonthNameMap() {
        String[] fullNames = {
                "january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december"
        };
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < fullNames.length; i++) {
            map.put(fullNames[i], i + 1);
            map.put(fullNames[i].substring(0, 3), i + 1);
        }
        return map;
    }

    /**
     * Recognises a "Month Year" search query in either token order (e.g. "August 2025" or
     * "2025 August"), the abbreviated month name, or a numeric month/year pair separated by a
     * space, slash or dash (e.g. "08/2025"). Returns {@code null} unless both a month and a
     * 4-digit year were found, so a query naming only one of the two falls back to title search.
     */
    private static int[] parseMonthYear(String query) {
        String normalized = query.trim().toLowerCase().replaceAll("[,/\\-]", " ").replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }
        Integer year = null;
        Integer month = null;
        for (String token : normalized.split(" ")) {
            if (token.matches("\\d{4}")) {
                year = Integer.parseInt(token);
            } else if (MONTH_NAME_TO_NUMBER.containsKey(token)) {
                month = MONTH_NAME_TO_NUMBER.get(token);
            } else if (month == null && token.matches("\\d{1,2}")) {
                int candidate = Integer.parseInt(token);
                if (candidate >= 1 && candidate <= 12) {
                    month = candidate;
                }
            }
        }
        return (year != null && month != null) ? new int[]{year, month} : null;
    }

    @Override
    public PublicationDetailDto getById(String id) {
        ParsedId parsed = parseId(id);
        return toDetail(id, findViewableOrThrow(parsed, id));
    }

    @Override
    public PublicationDetailDto getByYearAndMonth(Integer year, Integer month, PublicationLanguage language) {
        String id = idOf(year, month, language);
        return toDetail(id, findViewableOrThrow(new ParsedId(year, month, language), id));
    }

    @Override
    public PublicationDetailDto getLatest(PublicationLanguage language) {
        Publication latest = publicationRepository.findAll(CATALOG_ORDER).stream()
                .filter(visibility.viewableByCaller())
                .filter(p -> p.getLanguage() == language)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No publications have been uploaded yet"));
        return toDetail(idOf(latest.getYear(), latest.getMonth(), latest.getLanguage()), latest);
    }

    @Override
    public List<PublicationSummaryDto> getWindow(Integer year, Integer month, PublicationLanguage language, int count) {
        return publicationRepository.findByYear(year, CATALOG_ORDER).stream()
                .filter(visibility.viewableByCaller())
                .filter(p -> p.getLanguage() == language && !p.getMonth().equals(month))
                .sorted(Comparator.comparingInt(Publication::getMonth))
                .limit(count)
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public StoredFile loadPdfFile(String id) {
        ParsedId parsed = parseId(id);
        Publication publication = findViewableOrThrow(parsed, id);
        Path path = buildPublicationDir(parsed.year(), parsed.month()).resolve(publication.getPdfFile());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("PDF file is missing on the server for publication " + id);
        }
        Resource resource = new FileSystemResource(path);
        return new StoredFile(resource, "application/pdf", downloadFilename(publication));
    }

    // Fixed "feedworld" prefix + that issue's own month/year/language, e.g.
    // "feedworld_08_2025_english.pdf" - what the browser saves the file as, distinct from the
    // feed_world_<language>.pdf name it is stored under on disk. The language suffix keeps the three
    // editions of the same month from downloading over one another under the same name.
    private static String downloadFilename(Publication publication) {
        return "feedworld_" + String.format("%02d", publication.getMonth())
                + "_" + publication.getYear()
                + "_" + publication.getLanguage().name().toLowerCase()
                + ".pdf";
    }

    @Override
    public StoredFile loadThumbnail(String id) {
        ParsedId parsed = parseId(id);
        Publication publication = findViewableOrThrow(parsed, id);
        String thumbnailFile = publication.getThumbnailFile();
        if (thumbnailFile == null) {
            throw new ResourceNotFoundException("No thumbnail available for publication " + id);
        }
        Path path = buildPublicationDir(parsed.year(), parsed.month()).resolve(thumbnailFile);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Thumbnail file is missing on the server for publication " + id);
        }
        // Covers are always PNG (see PublicationLanguage#thumbnailFileName).
        Resource resource = new FileSystemResource(path);
        return new StoredFile(resource, "image/png", "cover-" + id + ".png");
    }

    @Override
    public PublicationDetailDto uploadPublication(MultipartFile file, Integer year, Integer month,
                                                    PublicationLanguage language) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }
        if (year == null || year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("Year must be a 4-digit year, e.g. 2029");
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (language == null) {
            throw new IllegalArgumentException("Language is required");
        }
        // A row whose PDF is gone from disk counts as not uploaded (the admin dashboard offers
        // "Upload" for it), so uploading fills that row in instead of being rejected as a duplicate.
        Publication existing = publicationRepository.findByYearAndMonthAndLanguage(year, month, language).orElse(null);
        if (existing != null && pdfExists(existing)) {
            throw new IllegalArgumentException(
                    "A " + language.name() + " publication for " + year + "-" + month + " already exists");
        }

        // storage/publications/<year>/<month>/feed_world_<language>.{pdf,png} - the month's three
        // editions share the folder, told apart by the language in their name. The cover is
        // always a PNG.
        Path publicationDir = buildPublicationDir(year, month);
        Path pdfTarget = publicationDir.resolve(language.pdfFileName());
        Path thumbnailTarget = publicationDir.resolve(language.thumbnailFileName());

        try {
            if (existing != null && existing.getThumbnailFile() != null) {
                // The missing PDF's leftover cover, if any - the new one is generated below.
                deleteQuietly(publicationDir.resolve(existing.getThumbnailFile()));
            }
            Files.createDirectories(publicationDir);
            file.transferTo(pdfTarget);

            PdfProcessingService.PdfMetadata metadata = pdfProcessingService.process(pdfTarget, thumbnailTarget);

            Publication publication = existing != null ? existing : new Publication();
            publication.setYear(year);
            publication.setMonth(month);
            publication.setLanguage(language);
            // Also resets the title when refilling a row whose PDF had gone missing.
            publication.setTitle(Publication.TITLE);
            publication.setPageCount(metadata.pageCount());
            publication.setPublishedDate(LocalDate.of(year, month, 1));
            publication.setPdfFile(pdfTarget.getFileName().toString());
            publication.setThumbnailFile(Files.exists(thumbnailTarget) ? thumbnailTarget.getFileName().toString() : null);

            publication = publicationRepository.save(publication);
            return toDetail(idOf(year, month, language), publication);
        } catch (IOException e) {
            deleteQuietly(pdfTarget);
            deleteQuietly(thumbnailTarget);
            throw new RuntimeException("Failed to store the publication file: " + e.getMessage(), e);
        }
    }

    @Override
    public PublicationDetailDto replacePdf(String id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        ParsedId parsed = parseId(id);
        Publication publication = findEntityOrThrow(parsed, id);
        Path publicationDir = buildPublicationDir(parsed.year(), parsed.month());

        // Remove the old PDF/thumbnail before writing the new ones, so a replaced issue never
        // leaves its previous files orphaned on disk. Only the two files named on this row are
        // deleted - the month's other editions share the folder.
        if (publication.getPdfFile() != null) {
            deleteQuietly(publicationDir.resolve(publication.getPdfFile()));
        }
        if (publication.getThumbnailFile() != null) {
            deleteQuietly(publicationDir.resolve(publication.getThumbnailFile()));
        }

        Path pdfTarget = publicationDir.resolve(publication.getLanguage().pdfFileName());
        Path thumbnailTarget = publicationDir.resolve(publication.getLanguage().thumbnailFileName());

        try {
            Files.createDirectories(publicationDir);
            file.transferTo(pdfTarget);

            PdfProcessingService.PdfMetadata metadata = pdfProcessingService.process(pdfTarget, thumbnailTarget);

            publication.setPageCount(metadata.pageCount());
            publication.setPdfFile(pdfTarget.getFileName().toString());
            publication.setThumbnailFile(Files.exists(thumbnailTarget) ? thumbnailTarget.getFileName().toString() : null);

            publication = publicationRepository.save(publication);
            return toDetail(id, publication);
        } catch (IOException e) {
            deleteQuietly(pdfTarget);
            deleteQuietly(thumbnailTarget);
            throw new RuntimeException("Failed to replace the publication file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePublication(String id) {
        ParsedId parsed = parseId(id);
        Publication publication = findEntityOrThrow(parsed, id);
        Path publicationDir = buildPublicationDir(parsed.year(), parsed.month());
        if (publication.getPdfFile() != null) {
            deleteQuietly(publicationDir.resolve(publication.getPdfFile()));
        }
        if (publication.getThumbnailFile() != null) {
            deleteQuietly(publicationDir.resolve(publication.getThumbnailFile()));
        }
        publicationRepository.delete(publication);
    }

    private Publication findEntityOrThrow(ParsedId parsed, String id) {
        return publicationRepository.findByYearAndMonthAndLanguage(parsed.year(), parsed.month(), parsed.language())
                .orElseThrow(() -> new ResourceNotFoundException("No publication found for " + id));
    }

    /**
     * {@link #findEntityOrThrow} for the reader-facing paths: an issue the caller may not see yet
     * (a future month, for a normal user) gets the very same 404 as one that doesn't exist, so the
     * API never reveals what has been uploaded ahead of release. Replace/Delete are admin-only and
     * keep using findEntityOrThrow.
     */
    private Publication findViewableOrThrow(ParsedId parsed, String id) {
        Publication publication = findEntityOrThrow(parsed, id);
        if (!visibility.canView(publication)) {
            throw new ResourceNotFoundException("No publication found for " + id);
        }
        return publication;
    }

    /**
     * The one place that turns (year, month) into a filesystem folder - every
     * upload/replace/delete/view/download/thumbnail path above resolves its files through this
     * method rather than building the path inline, so the on-disk layout only has one definition.
     */
    private Path buildPublicationDir(int year, int month) {
        return Paths.get(baseDir, String.valueOf(year), String.format("%02d", month));
    }

    /** {@code id} is "{year}-{month}-{language}", e.g. "2025-08-English" - see class javadoc. */
    private record ParsedId(int year, int month, PublicationLanguage language) {}

    private static ParsedId parseId(String id) {
        if (id != null) {
            String[] parts = id.split("-");
            if (parts.length == 3) {
                try {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    PublicationLanguage language = PublicationLanguage.valueOf(parts[2]);
                    return new ParsedId(year, month, language);
                } catch (IllegalArgumentException ignored) {
                    // falls through to the exception below
                }
            }
        }
        throw new ResourceNotFoundException("Publication not found: " + id);
    }

    private static String idOf(int year, int month, PublicationLanguage language) {
        return year + "-" + String.format("%02d", month) + "-" + language.name();
    }

    private PublicationSummaryDto toSummary(Publication p) {
        return PublicationSummaryDto.from(idOf(p.getYear(), p.getMonth(), p.getLanguage()), p, pdfExists(p));
    }

    private PublicationDetailDto toDetail(String id, Publication p) {
        return PublicationDetailDto.from(id, p, pdfExists(p));
    }

    /** Whether the PDF this row names is actually in its month folder - a row can outlive its file. */
    private boolean pdfExists(Publication p) {
        return p.getPdfFile() != null
                && Files.exists(buildPublicationDir(p.getYear(), p.getMonth()).resolve(p.getPdfFile()));
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; a leftover file on disk is not worth failing the request for.
        }
    }
}
