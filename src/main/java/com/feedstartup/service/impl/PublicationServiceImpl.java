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
import java.util.List;
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
        return publicationRepository.findByTitleContainingIgnoreCaseOrderByYearDescMonthDesc(query).stream()
                .map(PublicationSummaryDto::from)
                .collect(Collectors.toList());
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
