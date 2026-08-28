package com.feedstartup.service.impl;

import com.feedstartup.dto.PublicationDetailDto;
import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.Publication;
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

@Service
public class PublicationServiceImpl implements PublicationService {

    private final PublicationRepository publicationRepository;
    private final PdfProcessingService pdfProcessingService;

    @Value("${feedworld.storage.base-dir}")
    private String baseDir;

    @Autowired
    public PublicationServiceImpl(PublicationRepository publicationRepository,
                                   PdfProcessingService pdfProcessingService) {
        this.publicationRepository = publicationRepository;
        this.pdfProcessingService = pdfProcessingService;
    }

    @Override
    public List<YearSummaryDto> listYears() {
        return publicationRepository.countGroupedByYear().stream()
                .map(row -> new YearSummaryDto(row.getYear(), row.getCount()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicationSummaryDto> listByYear(Integer year) {
        return publicationRepository.findByYearOrderByMonthDesc(year).stream()
                .map(PublicationSummaryDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<PublicationSummaryDto> search(String query) {
        if (query == null || query.isBlank()) {
            return publicationRepository.findAllByOrderByYearDescMonthDesc().stream()
                    .map(PublicationSummaryDto::from)
                    .collect(Collectors.toList());
        }

        // The archive search box is meant to be driven by "Month Year" (e.g. "August 2025", also
        // accepting the abbreviated month name and either token order) so a reader can jump
        // straight to one issue rather than hunting for it by title. Only a query where both a
        // month and a year were recognised takes this path; anything else - including a bare
        // year or a bare month name - falls through to the plain title search below.
        int[] monthYear = parseMonthYear(query);
        if (monthYear != null) {
            return publicationRepository.findByYearAndMonth(monthYear[0], monthYear[1])
                    .map(PublicationSummaryDto::from)
                    .map(List::of)
                    .orElseGet(List::of);
        }

        return publicationRepository.findByTitleContainingIgnoreCaseOrderByYearDescMonthDesc(query).stream()
                .map(PublicationSummaryDto::from)
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
    public PublicationDetailDto getById(Long id) {
        return PublicationDetailDto.from(findPublicationOrThrow(id));
    }

    @Override
    public PublicationDetailDto getByYearAndMonth(Integer year, Integer month) {
        Publication publication = publicationRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No publication found for " + year + "-" + month));
        return PublicationDetailDto.from(publication);
    }

    @Override
    public PublicationDetailDto getLatest() {
        Publication latest = publicationRepository.findAllByOrderByYearDescMonthDesc().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No publications have been uploaded yet"));
        return PublicationDetailDto.from(latest);
    }

    @Override
    public List<PublicationSummaryDto> getWindow(Integer year, Integer month, int count) {
        int anchorIndex = year * 12 + (month - 1);
        int yearEndIndex = year * 12 + 11;
        int maxIndex = Math.min(yearEndIndex, anchorIndex + count);
        return publicationRepository.findAllByOrderByYearDescMonthDesc().stream()
                .filter(p -> {
                    int index = p.getYear() * 12 + (p.getMonth() - 1);
                    return index > anchorIndex && index <= maxIndex;
                })
                .sorted(Comparator.comparingInt(p -> p.getYear() * 12 + p.getMonth()))
                .map(PublicationSummaryDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public StoredFile loadPdfFile(Long id) {
        Publication publication = findPublicationOrThrow(id);
        Path path = Paths.get(publication.getPdfPath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("PDF file is missing on the server for publication " + id);
        }
        Resource resource = new FileSystemResource(path);
        String filename = publication.getTitle().replaceAll("\\s+", "_")
                + "_" + publication.getYear()
                + "_" + String.format("%02d", publication.getMonth()) + ".pdf";
        return new StoredFile(resource, "application/pdf", filename);
    }

    @Override
    public StoredFile loadThumbnail(Long id) {
        Publication publication = findPublicationOrThrow(id);
        if (publication.getThumbnailPath() == null) {
            throw new ResourceNotFoundException("No thumbnail available for publication " + id);
        }
        Path path = Paths.get(publication.getThumbnailPath());
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
        if (year == null || year < 2000 || year > 2100) {
            throw new IllegalArgumentException("A valid year is required");
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        if (publicationRepository.findByYearAndMonth(year, month).isPresent()) {
            throw new IllegalArgumentException("A publication for " + year + "-" + month + " already exists");
        }

        Path pdfDir = Paths.get(baseDir, "pdf");
        Path thumbnailDir = Paths.get(baseDir, "thumbnails");
        String storedFileName = year + "-" + String.format("%02d", month) + "-" + UUID.randomUUID();
        Path pdfTarget = pdfDir.resolve(storedFileName + ".pdf");
        Path thumbnailTarget = thumbnailDir.resolve(storedFileName + ".png");

        try {
            Files.createDirectories(pdfDir);
            file.transferTo(pdfTarget);

            PdfProcessingService.PdfMetadata metadata = pdfProcessingService.process(pdfTarget, thumbnailTarget);

            Publication publication = new Publication();
            publication.setTitle(title != null && !title.isBlank() ? title : "Feed World");
            publication.setYear(year);
            publication.setMonth(month);
            publication.setVolume(volume);
            publication.setIssueNumber(issueNumber);
            publication.setPageCount(metadata.pageCount());
            publication.setPublishedDate(LocalDate.of(year, month, 1));
            publication.setPdfPath(pdfTarget.toString());
            publication.setThumbnailPath(Files.exists(thumbnailTarget) ? thumbnailTarget.toString() : null);
            publication.setFileSizeBytes(Files.size(pdfTarget));

            Publication saved = publicationRepository.save(publication);
            return PublicationDetailDto.from(saved);
        } catch (IOException e) {
            deleteQuietly(pdfTarget);
            deleteQuietly(thumbnailTarget);
            throw new RuntimeException("Failed to store the publication file: " + e.getMessage(), e);
        }
    }

    @Override
    public PublicationDetailDto updateMetadata(Long id, String title, Integer volume, Integer issueNumber) {
        Publication publication = findPublicationOrThrow(id);
        if (title != null && !title.isBlank()) {
            publication.setTitle(title);
        }
        if (volume != null) {
            publication.setVolume(volume);
        }
        if (issueNumber != null) {
            publication.setIssueNumber(issueNumber);
        }
        return PublicationDetailDto.from(publicationRepository.save(publication));
    }

    @Override
    public void deletePublication(Long id) {
        Publication publication = findPublicationOrThrow(id);
        deleteQuietly(Paths.get(publication.getPdfPath()));
        if (publication.getThumbnailPath() != null) {
            deleteQuietly(Paths.get(publication.getThumbnailPath()));
        }
        publicationRepository.delete(publication);
    }

    private Publication findPublicationOrThrow(Long id) {
        return publicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publication not found: " + id));
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; a leftover file on disk is not worth failing the request for.
        }
    }
}
