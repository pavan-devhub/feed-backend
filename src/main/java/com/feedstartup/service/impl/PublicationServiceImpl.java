package com.feedstartup.service.impl;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.PublicationMeta;
import com.feedstartup.service.PdfProcessingService;
import com.feedstartup.service.PublicationService;
import com.feedstartup.service.StoredFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * No database is involved anywhere in here - the publication catalog IS the folder tree under
 * {@code feedworld.storage.base-dir}: {@code <year>/<month>/} holds one issue's PDF, cover
 * thumbnail, and a {@code meta.json} sidecar with its title/volume/issue number/page count/etc.
 * Listing, looking up, and deleting an issue all work by walking that tree - there is nothing
 * else to keep in sync. An issue's public id is its "{year}-{month}" folder path (e.g. "2025-08").
 */
@Service
public class PublicationServiceImpl implements PublicationService {

    // Feed World only publishes for this rolling two-year window (mirrors the frontend's
    // YEAR_OPTIONS in PublicationsHub.jsx) - reject anything outside it at upload time rather
    // than letting stray years accumulate in storage.
    private static final int MIN_YEAR = 2025;
    private static final int MAX_YEAR = 2026;

    private static final String META_FILE = "meta.json";

    private final PdfProcessingService pdfProcessingService;
    private final ObjectMapper objectMapper;

    @Value("${feedworld.storage.base-dir}")
    private String baseDir;

    @Autowired
    public PublicationServiceImpl(PdfProcessingService pdfProcessingService, ObjectMapper objectMapper) {
        this.pdfProcessingService = pdfProcessingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<YearSummaryDto> listYears() {
        Map<Integer, Long> counts = listAllEntries().stream()
                .collect(Collectors.groupingBy(Entry::year, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByKey().reversed())
                .map(e -> new YearSummaryDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicationSummaryDto> listByYear(Integer year) {
        return listAllEntries().stream()
                .filter(e -> e.year().equals(year))
                .sorted(Comparator.comparingInt(Entry::month).reversed())
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicationSummaryDto> search(String query) {
        if (query == null || query.isBlank()) {
            return sortDesc(listAllEntries()).stream()
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        }

        // The archive search box is meant to be driven by "Month Year" (e.g. "August 2025", also
        // accepting the abbreviated month name and either token order) so a reader can jump
        // straight to one issue rather than hunting for it by title. Only a query where both a
        // month and a year were recognised takes this path; anything else - including a bare
        // year or a bare month name - falls through to the plain title search below.
        int[] monthYear = parseMonthYear(query);
        if (monthYear != null) {
            return listAllEntries().stream()
                    .filter(e -> e.year() == monthYear[0] && e.month() == monthYear[1])
                    .findFirst()
                    .map(e -> List.of(toSummary(e)))
                    .orElseGet(List::of);
        }

        String needle = query.toLowerCase();
        return sortDesc(listAllEntries()).stream()
                .filter(e -> e.meta().getTitle() != null && e.meta().getTitle().toLowerCase().contains(needle))
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
        int[] ym = parseId(id);
        return toDetail(findEntryOrThrow(ym[0], ym[1], id));
    }

    @Override
    public PublicationDetailDto getByYearAndMonth(Integer year, Integer month) {
        return toDetail(findEntryOrThrow(year, month, year + "-" + month));
    }

    @Override
    public PublicationDetailDto getLatest() {
        Entry latest = sortDesc(listAllEntries()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No publications have been uploaded yet"));
        return toDetail(latest);
    }

    @Override
    public List<PublicationSummaryDto> getWindow(Integer year, Integer month, int count) {
        return listAllEntries().stream()
                .filter(e -> e.year().equals(year) && !e.month().equals(month))
                .sorted(Comparator.comparingInt(Entry::month))
                .limit(count)
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public StoredFile loadPdfFile(String id) {
        int[] ym = parseId(id);
        Entry entry = findEntryOrThrow(ym[0], ym[1], id);
        String pdfFile = entry.meta().getPdfFile();
        Path path = pdfFile == null ? null : monthDir(ym[0], ym[1]).resolve(pdfFile);
        if (path == null || !Files.exists(path)) {
            throw new ResourceNotFoundException("PDF file is missing on the server for publication " + id);
        }
        Resource resource = new FileSystemResource(path);
        String filename = entry.meta().getTitle().replaceAll("\\s+", "_")
                + "_" + ym[0]
                + "_" + String.format("%02d", ym[1]) + ".pdf";
        return new StoredFile(resource, "application/pdf", filename);
    }

    @Override
    public StoredFile loadThumbnail(String id) {
        int[] ym = parseId(id);
        Entry entry = findEntryOrThrow(ym[0], ym[1], id);
        String thumbnailFile = entry.meta().getThumbnailFile();
        if (thumbnailFile == null) {
            throw new ResourceNotFoundException("No thumbnail available for publication " + id);
        }
        Path path = monthDir(ym[0], ym[1]).resolve(thumbnailFile);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Thumbnail file is missing on the server for publication " + id);
        }
        Resource resource = new FileSystemResource(path);
        return new StoredFile(resource, "image/png", "cover-" + id + ".png");
    }

    @Override
    public PublicationDetailDto uploadPublication(MultipartFile file, String title, Integer year, Integer month,
                                                    Integer volume, Integer issueNumber) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }
        if (year == null || year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("Year must be " + MIN_YEAR + " or " + MAX_YEAR);
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }

        // storage/publications/<year>/<month>/<random-name>.{pdf,png} - one folder per calendar
        // month so a year's twelve issues never share a directory with another year's, and the
        // random name keeps a PDF's on-disk filename from leaking its title/date to anyone who
        // gets a raw path.
        Path monthDir = monthDir(year, month);
        Path metaPath = monthDir.resolve(META_FILE);
        if (Files.exists(metaPath)) {
            throw new IllegalArgumentException("A publication for " + year + "-" + month + " already exists");
        }

        String storedFileName = UUID.randomUUID().toString();
        Path pdfTarget = monthDir.resolve(storedFileName + ".pdf");
        Path thumbnailTarget = monthDir.resolve(storedFileName + ".png");

        try {
            Files.createDirectories(monthDir);
            file.transferTo(pdfTarget);

            PdfProcessingService.PdfMetadata metadata = pdfProcessingService.process(pdfTarget, thumbnailTarget);

            PublicationMeta meta = new PublicationMeta();
            meta.setTitle(title != null && !title.isBlank() ? title : "Feed World");
            meta.setVolume(volume);
            meta.setIssueNumber(issueNumber);
            meta.setPageCount(metadata.pageCount());
            meta.setPublishedDate(LocalDate.of(year, month, 1));
            meta.setPdfFile(pdfTarget.getFileName().toString());
            meta.setThumbnailFile(Files.exists(thumbnailTarget) ? thumbnailTarget.getFileName().toString() : null);
            meta.setFileSizeBytes(Files.size(pdfTarget));
            meta.setCreatedAt(LocalDateTime.now());
            meta.setUpdatedAt(meta.getCreatedAt());

            writeMeta(metaPath, meta);
            return toDetail(new Entry(year, month, meta));
        } catch (IOException e) {
            deleteQuietly(pdfTarget);
            deleteQuietly(thumbnailTarget);
            throw new RuntimeException("Failed to store the publication file: " + e.getMessage(), e);
        }
    }

    @Override
    public PublicationDetailDto updateMetadata(String id, String title, Integer volume, Integer issueNumber) {
        int[] ym = parseId(id);
        Path metaPath = monthDir(ym[0], ym[1]).resolve(META_FILE);
        PublicationMeta meta = findEntryOrThrow(ym[0], ym[1], id).meta();
        if (title != null && !title.isBlank()) {
            meta.setTitle(title);
        }
        if (volume != null) {
            meta.setVolume(volume);
        }
        if (issueNumber != null) {
            meta.setIssueNumber(issueNumber);
        }
        meta.setUpdatedAt(LocalDateTime.now());
        writeMeta(metaPath, meta);
        return toDetail(new Entry(ym[0], ym[1], meta));
    }

    @Override
    public void deletePublication(String id) {
        int[] ym = parseId(id);
        Path monthDir = monthDir(ym[0], ym[1]);
        Entry entry = findEntryOrThrow(ym[0], ym[1], id);
        if (entry.meta().getPdfFile() != null) {
            deleteQuietly(monthDir.resolve(entry.meta().getPdfFile()));
        }
        if (entry.meta().getThumbnailFile() != null) {
            deleteQuietly(monthDir.resolve(entry.meta().getThumbnailFile()));
        }
        deleteQuietly(monthDir.resolve(META_FILE));
    }

    // --- folder-tree scanning -------------------------------------------------------------

    private List<Entry> listAllEntries() {
        Path base = Paths.get(baseDir);
        if (!Files.isDirectory(base)) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>();
        for (Path yearDir : listSubdirectories(base)) {
            Integer year = parseIntOrNull(yearDir.getFileName().toString());
            if (year == null) continue;
            for (Path monthDir : listSubdirectories(yearDir)) {
                Integer month = parseIntOrNull(monthDir.getFileName().toString());
                if (month == null) continue;
                Path metaPath = monthDir.resolve(META_FILE);
                if (!Files.exists(metaPath)) continue;
                entries.add(new Entry(year, month, readMeta(metaPath)));
            }
        }
        return entries;
    }

    private Entry findEntryOrThrow(int year, int month, String id) {
        Path metaPath = monthDir(year, month).resolve(META_FILE);
        if (!Files.exists(metaPath)) {
            throw new ResourceNotFoundException("No publication found for " + id);
        }
        return new Entry(year, month, readMeta(metaPath));
    }

    private Path monthDir(int year, int month) {
        return Paths.get(baseDir, String.valueOf(year), String.format("%02d", month));
    }

    private static List<Path> listSubdirectories(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list " + dir + ": " + e.getMessage(), e);
        }
    }

    private static List<Entry> sortDesc(List<Entry> entries) {
        return entries.stream()
                .sorted(Comparator.comparingInt(Entry::year).thenComparingInt(Entry::month).reversed())
                .collect(Collectors.toList());
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int[] parseId(String id) {
        if (id != null) {
            String[] parts = id.split("-");
            if (parts.length == 2) {
                try {
                    return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
                } catch (NumberFormatException ignored) {
                    // falls through to the exception below
                }
            }
        }
        throw new ResourceNotFoundException("Publication not found: " + id);
    }

    private static String idOf(int year, int month) {
        return year + "-" + String.format("%02d", month);
    }

    private PublicationSummaryDto toSummary(Entry e) {
        return PublicationSummaryDto.from(idOf(e.year(), e.month()), e.year(), e.month(), e.meta());
    }

    private PublicationDetailDto toDetail(Entry e) {
        return PublicationDetailDto.from(idOf(e.year(), e.month()), e.year(), e.month(), e.meta());
    }

    private PublicationMeta readMeta(Path metaPath) {
        try {
            return objectMapper.readValue(metaPath.toFile(), PublicationMeta.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to read publication metadata at " + metaPath + ": " + e.getMessage(), e);
        }
    }

    private void writeMeta(Path metaPath, PublicationMeta meta) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metaPath.toFile(), meta);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to write publication metadata: " + e.getMessage(), e);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; a leftover file on disk is not worth failing the request for.
        }
    }

    private record Entry(Integer year, Integer month, PublicationMeta meta) {}
}
