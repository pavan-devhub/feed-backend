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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The publication catalog is the {@code publications} table (see {@link Publication} /
 * {@link PublicationRepository}) - listing, search and lookups all query it directly. Every
 * month has up to three rows, one per {@link PublicationLanguage} - English, Telugu and Hindi are
 * separate PDFs, not one PDF with three translations. Each row's PDF and cover thumbnail are
 * plain files on disk under {@code storage/publications/<year>/<language>/<month>/} (see
 * {@code feedworld.storage.base-dir} and {@link #buildPublicationDir}) - language comes before
 * month so each edition's files live in their own folder and Replace/Delete on one language can
 * never touch another's; the row only stores their filenames, resolved against that folder
 * whenever the PDF or thumbnail is actually fetched. An issue's public id is its
 * "{year}-{month}-{language}" path (e.g. "2025-08-English"). Publications written before this
 * layout existed (plain {@code <year>/<month>/}) are moved into place by
 * {@link com.feedstartup.config.PublicationStorageMigrationRunner} on startup.
 */
@Service
public class PublicationServiceImpl implements PublicationService {

    // Reject years earlier than this at upload time. No fixed upper bound - maxAllowedYear()
    // always allows a couple of years ahead of "today".
    private static final int MIN_YEAR = 2025;
    private static final int YEARS_AHEAD_ALLOWED = 2;

    private final PdfProcessingService pdfProcessingService;
    private final PublicationRepository publicationRepository;

    @Value("${feedworld.storage.base-dir}")
    private String baseDir;

    @Autowired
    public PublicationServiceImpl(PdfProcessingService pdfProcessingService, PublicationRepository publicationRepository) {
        this.pdfProcessingService = pdfProcessingService;
        this.publicationRepository = publicationRepository;
    }

    @Override
    public List<YearSummaryDto> listYears() {
        Map<Integer, Long> counts = publicationRepository.findAll().stream()
                .collect(Collectors.groupingBy(Publication::getYear, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByKey().reversed())
                .map(e -> new YearSummaryDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicationSummaryDto> listByYear(Integer year) {
        return publicationRepository.findByYear(year).stream()
                .sorted(byMonthDescThenLanguage())
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicationSummaryDto> search(String query) {
        if (query == null || query.isBlank()) {
            return sortDesc(publicationRepository.findAll()).stream()
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        }

        // The archive search box is meant to be driven by "Month Year" (e.g. "August 2025", also
        // accepting the abbreviated month name and either token order) so a reader can jump
        // straight to one issue rather than hunting for it by title. Only a query where both a
        // month and a year were recognised takes this path; anything else - including a bare
        // year or a bare month name - falls through to the plain title search below. A month/year
        // match can return up to three rows (one per language).
        int[] monthYear = parseMonthYear(query);
        if (monthYear != null) {
            return publicationRepository.findByYearAndMonth(monthYear[0], monthYear[1]).stream()
                    .sorted(Comparator.comparing(Publication::getLanguage))
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        }

        return sortDesc(publicationRepository.findByTitleContainingIgnoreCase(query)).stream()
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
        return toDetail(id, findEntityOrThrow(parsed, id));
    }

    @Override
    public PublicationDetailDto getByYearAndMonth(Integer year, Integer month, PublicationLanguage language) {
        String id = idOf(year, month, language);
        return toDetail(id, findEntityOrThrow(new ParsedId(year, month, language), id));
    }

    @Override
    public PublicationDetailDto getLatest(PublicationLanguage language) {
        Publication latest = sortDesc(publicationRepository.findAll()).stream()
                .filter(p -> p.getLanguage() == language)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No publications have been uploaded yet"));
        return toDetail(idOf(latest.getYear(), latest.getMonth(), latest.getLanguage()), latest);
    }

    @Override
    public List<PublicationSummaryDto> getWindow(Integer year, Integer month, PublicationLanguage language, int count) {
        return publicationRepository.findByYear(year).stream()
                .filter(p -> p.getLanguage() == language && !p.getMonth().equals(month))
                .sorted(Comparator.comparingInt(Publication::getMonth))
                .limit(count)
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public StoredFile loadPdfFile(String id) {
        ParsedId parsed = parseId(id);
        Publication publication = findEntityOrThrow(parsed, id);
        Path path = buildPublicationDir(parsed.year(), parsed.language(), parsed.month()).resolve(publication.getPdfFile());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("PDF file is missing on the server for publication " + id);
        }
        Resource resource = new FileSystemResource(path);
        return new StoredFile(resource, "application/pdf", downloadFilename(publication));
    }

    // Fixed "feedworld" prefix + that issue's own month/year/language, e.g.
    // "feedworld_08_2025_english.pdf" - what the browser saves the file as, distinct from the
    // random UUID name it is actually stored under on disk. The language suffix keeps the three
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
        Publication publication = findEntityOrThrow(parsed, id);
        String thumbnailFile = publication.getThumbnailFile();
        if (thumbnailFile == null) {
            throw new ResourceNotFoundException("No thumbnail available for publication " + id);
        }
        Path path = buildPublicationDir(parsed.year(), parsed.language(), parsed.month()).resolve(thumbnailFile);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Thumbnail file is missing on the server for publication " + id);
        }
        Resource resource = new FileSystemResource(path);
        String extension = extensionOf(thumbnailFile);
        return new StoredFile(resource, contentTypeFor(extension), "cover-" + id + extension);
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private static String contentTypeFor(String extension) {
        return switch (extension.toLowerCase()) {
            case ".avif" -> "image/avif";
            case ".webp" -> "image/webp";
            case ".jpg", ".jpeg" -> "image/jpeg";
            default -> "image/png";
        };
    }

    @Override
    public PublicationDetailDto uploadPublication(MultipartFile file, String title, Integer year, Integer month,
                                                    PublicationLanguage language, Integer volume, Integer issueNumber) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }
        int maxYear = maxAllowedYear();
        if (year == null || year < MIN_YEAR || year > maxYear) {
            throw new IllegalArgumentException("Year must be between " + MIN_YEAR + " and " + maxYear);
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (language == null) {
            throw new IllegalArgumentException("Language is required");
        }
        if (publicationRepository.findByYearAndMonthAndLanguage(year, month, language).isPresent()) {
            throw new IllegalArgumentException(
                    "A " + language.name() + " publication for " + year + "-" + month + " already exists");
        }

        // storage/publications/<year>/<language>/<month>/<random-name>.{pdf,png} - random name
        // keeps the PDF's title/date off the raw path. Own folder per language so Replace/Delete
        // on one edition never touches another's files.
        Path publicationDir = buildPublicationDir(year, language, month);
        String storedFileName = UUID.randomUUID().toString();
        Path pdfTarget = publicationDir.resolve(storedFileName + ".pdf");
        Path thumbnailTarget = publicationDir.resolve(storedFileName + ".png");

        try {
            Files.createDirectories(publicationDir);
            file.transferTo(pdfTarget);

            PdfProcessingService.PdfMetadata metadata = pdfProcessingService.process(pdfTarget, thumbnailTarget);

            Publication publication = new Publication();
            publication.setYear(year);
            publication.setMonth(month);
            publication.setLanguage(language);
            publication.setTitle(title != null && !title.isBlank() ? title : "Feed World");
            publication.setVolume(volume);
            publication.setIssueNumber(issueNumber);
            publication.setPageCount(metadata.pageCount());
            publication.setPublishedDate(LocalDate.of(year, month, 1));
            publication.setPdfFile(pdfTarget.getFileName().toString());
            publication.setThumbnailFile(Files.exists(thumbnailTarget) ? thumbnailTarget.getFileName().toString() : null);
            publication.setFileSizeBytes(Files.size(pdfTarget));

            publication = publicationRepository.save(publication);
            return toDetail(idOf(year, month, language), publication);
        } catch (IOException e) {
            deleteQuietly(pdfTarget);
            deleteQuietly(thumbnailTarget);
            throw new RuntimeException("Failed to store the publication file: " + e.getMessage(), e);
        }
    }

    @Override
    public PublicationDetailDto updateMetadata(String id, String title, Integer volume, Integer issueNumber) {
        ParsedId parsed = parseId(id);
        Publication publication = findEntityOrThrow(parsed, id);
        if (title != null && !title.isBlank()) {
            publication.setTitle(title);
        }
        if (volume != null) {
            publication.setVolume(volume);
        }
        if (issueNumber != null) {
            publication.setIssueNumber(issueNumber);
        }
        publication = publicationRepository.save(publication);
        return toDetail(id, publication);
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
        Path publicationDir = buildPublicationDir(parsed.year(), publication.getLanguage(), parsed.month());

        // Remove the old PDF/thumbnail before writing the new ones under fresh random names, so
        // a replaced issue never leaves its previous files orphaned on disk. Only this language's
        // folder is touched - the other editions' files live in their own language folder.
        if (publication.getPdfFile() != null) {
            deleteQuietly(publicationDir.resolve(publication.getPdfFile()));
        }
        if (publication.getThumbnailFile() != null) {
            deleteQuietly(publicationDir.resolve(publication.getThumbnailFile()));
        }

        String storedFileName = UUID.randomUUID().toString();
        Path pdfTarget = publicationDir.resolve(storedFileName + ".pdf");
        Path thumbnailTarget = publicationDir.resolve(storedFileName + ".png");

        try {
            Files.createDirectories(publicationDir);
            file.transferTo(pdfTarget);

            PdfProcessingService.PdfMetadata metadata = pdfProcessingService.process(pdfTarget, thumbnailTarget);

            publication.setPageCount(metadata.pageCount());
            publication.setPdfFile(pdfTarget.getFileName().toString());
            publication.setThumbnailFile(Files.exists(thumbnailTarget) ? thumbnailTarget.getFileName().toString() : null);
            publication.setFileSizeBytes(Files.size(pdfTarget));

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
        Path publicationDir = buildPublicationDir(parsed.year(), publication.getLanguage(), parsed.month());
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
     * The one place that turns (year, language, month) into a filesystem folder - every
     * upload/replace/delete/view/download/thumbnail path above resolves its files through this
     * method rather than building the path inline, so the on-disk layout only has one definition.
     */
    private Path buildPublicationDir(int year, PublicationLanguage language, int month) {
        return Paths.get(baseDir, String.valueOf(year), language.name().toLowerCase(), String.format("%02d", month));
    }

    private static int maxAllowedYear() {
        return LocalDate.now().getYear() + YEARS_AHEAD_ALLOWED;
    }

    private static List<Publication> sortDesc(List<Publication> entries) {
        return entries.stream()
                .sorted(Comparator.comparingInt(Publication::getYear).thenComparingInt(Publication::getMonth).reversed())
                .collect(Collectors.toList());
    }

    private static Comparator<Publication> byMonthDescThenLanguage() {
        return Comparator.comparingInt(Publication::getMonth).reversed()
                .thenComparing(Publication::getLanguage);
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
        return PublicationSummaryDto.from(idOf(p.getYear(), p.getMonth(), p.getLanguage()), p);
    }

    private PublicationDetailDto toDetail(String id, Publication p) {
        return PublicationDetailDto.from(id, p);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; a leftover file on disk is not worth failing the request for.
        }
    }
}
