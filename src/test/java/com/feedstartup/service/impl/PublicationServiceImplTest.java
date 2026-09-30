package com.feedstartup.service.impl;

import com.feedstartup.dto.PublicationSummaryDto;
import com.feedstartup.dto.YearSummaryDto;
import com.feedstartup.exception.ResourceNotFoundException;
import com.feedstartup.model.Publication;
import com.feedstartup.model.PublicationLanguage;
import com.feedstartup.repository.PublicationRepository;
import com.feedstartup.service.PdfProcessingService;
import com.feedstartup.service.PublicationVisibility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * With "today" pinned to 29 September 2026 and the admin having uploaded English issues from
 * 2025 through 2027: a normal user's view stops at September 2026, an admin's doesn't.
 */
class PublicationServiceImplTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @TempDir
    Path storage;

    private final PublicationRepository repository = mock(PublicationRepository.class);
    private final List<Publication> uploaded = new ArrayList<>();
    private PublicationServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        for (int month = 8; month <= 12; month++) upload(2025, month);
        for (int month = 1; month <= 12; month++) upload(2026, month);
        upload(2027, 1);

        Clock sept29 = Clock.fixed(LocalDateTime.of(2026, 9, 29, 12, 0).atZone(INDIA).toInstant(), INDIA);
        service = new PublicationServiceImpl(mock(PdfProcessingService.class), repository, new PublicationVisibility(sept29));
        ReflectionTestUtils.setField(service, "baseDir", storage.toString());

        when(repository.findAll(any(Sort.class))).thenAnswer(inv -> newestFirst());
        when(repository.findByYearAndLanguage(anyInt(), any(), any(Sort.class))).thenAnswer(inv -> uploaded.stream()
                .filter(p -> p.getYear().equals(inv.getArgument(0)) && p.getLanguage() == inv.getArgument(1))
                .toList());
        when(repository.findByYearAndMonthAndLanguage(anyInt(), anyInt(), any())).thenAnswer(inv -> uploaded.stream()
                .filter(p -> p.getYear().equals(inv.getArgument(0)) && p.getMonth().equals(inv.getArgument(1))
                        && p.getLanguage() == inv.getArgument(2))
                .findFirst());
    }

    @AfterEach
    void clearCaller() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aUsersYearListStopsAtTheCurrentYearAndCountsOnlyReleasedMonths() {
        asCaller("ROLE_USER");

        List<YearSummaryDto> years = service.listYears();

        assertEquals(List.of(2026, 2025), years.stream().map(YearSummaryDto::getYear).toList());
        assertEquals(9L, years.get(0).getCount());
        assertEquals(List.of(new YearSummaryDto.LanguageCount("English", 9)), years.get(0).getLanguages());
        assertEquals(5L, years.get(1).getCount());
    }

    @Test
    void aUsersCurrentYearShelfRunsJanuaryToSeptember() {
        asCaller("ROLE_USER");

        List<Integer> months = service.listByYear(2026, PublicationLanguage.English).stream()
                .map(PublicationSummaryDto::getMonth).toList();

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), months);
    }

    @Test
    void aUserSeesEveryIssueOfAPastYear() {
        asCaller("ROLE_USER");

        assertEquals(5, service.listByYear(2025, PublicationLanguage.English).size());
    }

    @Test
    void aUserCannotReachAnUnreleasedIssueDirectly() {
        asCaller("ROLE_USER");

        assertEquals("2026-09-English", service.getById("2026-09-English").getId());
        assertThrows(ResourceNotFoundException.class, () -> service.getById("2026-10-English"));
        assertThrows(ResourceNotFoundException.class, () -> service.loadPdfFile("2026-10-English"));
        assertThrows(ResourceNotFoundException.class, () -> service.loadThumbnail("2026-10-English"));
        assertThrows(ResourceNotFoundException.class, () -> service.loadPdfFile("2027-01-English"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.getByYearAndMonth(2026, 12, PublicationLanguage.English));
        assertEquals(List.of(), service.listByYear(2027, PublicationLanguage.English));
        assertEquals("2026-09-English", service.getLatest(PublicationLanguage.English).getId());
    }

    @Test
    void anAdminSeesIssuesUploadedAheadOfRelease() {
        asCaller("ROLE_ADMIN");

        assertEquals(List.of(2027, 2026, 2025), service.listYears().stream().map(YearSummaryDto::getYear).toList());
        assertEquals(12, service.listByYear(2026, PublicationLanguage.English).size());
        assertEquals("2026-10-English", service.getById("2026-10-English").getId());
        assertEquals("2027-01-English", service.getLatest(PublicationLanguage.English).getId());
    }

    private void upload(int year, int month) throws IOException {
        Publication publication = new Publication();
        publication.setYear(year);
        publication.setMonth(month);
        publication.setLanguage(PublicationLanguage.English);
        publication.setPdfFile("feed_world_english.pdf");
        publication.setThumbnailFile("feed_world_english.png");
        Path dir = Files.createDirectories(storage.resolve(String.valueOf(year)).resolve(String.format("%02d", month)));
        Files.writeString(dir.resolve("feed_world_english.pdf"), "%PDF");
        Files.writeString(dir.resolve("feed_world_english.png"), "png");
        uploaded.add(publication);
    }

    // What the repository's CATALOG_ORDER would return.
    private List<Publication> newestFirst() {
        return uploaded.stream()
                .sorted((a, b) -> a.getYear().equals(b.getYear())
                        ? b.getMonth() - a.getMonth()
                        : b.getYear() - a.getYear())
                .toList();
    }

    private static void asCaller(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "reader@feed.test", null, List.of(new SimpleGrantedAuthority(role))));
    }
}
