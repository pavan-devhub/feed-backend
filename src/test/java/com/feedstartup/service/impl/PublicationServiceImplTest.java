package com.feedstartup.service.impl;

import com.feedstartup.dto.AdminPublicationRowDto;
import com.feedstartup.dto.PageDto;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.feedstartup.dto.AdminPublicationRowDto.Status.NOT_PUBLISHED;
import static com.feedstartup.dto.AdminPublicationRowDto.Status.PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private final PdfProcessingService pdfProcessing = mock(PdfProcessingService.class);
    private final List<Publication> uploaded = new ArrayList<>();
    private PublicationServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        for (int month = 8; month <= 12; month++) upload(2025, month);
        for (int month = 1; month <= 12; month++) upload(2026, month);
        upload(2027, 1);

        Clock sept29 = Clock.fixed(LocalDateTime.of(2026, 9, 29, 12, 0).atZone(INDIA).toInstant(), INDIA);
        service = new PublicationServiceImpl(pdfProcessing, repository, new PublicationVisibility(sept29),
                mock(ApplicationEventPublisher.class));
        ReflectionTestUtils.setField(service, "baseDir", storage.toString());

        when(repository.findAll(any(Sort.class))).thenAnswer(inv -> newestFirst());
        when(repository.findByYearAndMonthBetween(anyInt(), anyInt(), anyInt(), any(Pageable.class))).thenAnswer(inv -> {
            Integer year = inv.getArgument(0);
            Integer fromMonth = inv.getArgument(1);
            Integer toMonth = inv.getArgument(2);
            Pageable pageable = inv.getArgument(3);
            List<Publication> matching = newestFirst().stream()
                    .filter(p -> p.getYear().equals(year) && p.getMonth() >= fromMonth && p.getMonth() <= toMonth)
                    .toList();
            int from = (int) Math.min(pageable.getOffset(), matching.size());
            int to = Math.min(from + pageable.getPageSize(), matching.size());
            return new PageImpl<>(matching.subList(from, to), pageable, matching.size());
        });
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

    @Test
    void theAdminTableListsOnlyTheMonthsAPastYearHasUploads() {
        // 2025 has issues from August to December only - every one of them already published.
        PageDto<AdminPublicationRowDto> year2025 = service.pageForAdmin(2025, null, null, 0, 12);

        assertEquals(List.of("12 PUBLISHED", "11 PUBLISHED", "10 PUBLISHED", "9 PUBLISHED", "8 PUBLISHED"),
                describe(year2025));
        assertEquals(5, year2025.total());
        assertEquals("2025-12-English", year2025.items().get(0).publication().getId());
    }

    @Test
    void inTheCurrentYearIssuesUploadedAheadOfTheirMonthAreNotPublishedYet() {
        // All of 2026 is uploaded; on 29 September, October to December are still to come.
        PageDto<AdminPublicationRowDto> first = service.pageForAdmin(2026, null, null, 0, 5);

        assertEquals(12, first.total());
        assertEquals(3, first.totalPages());
        assertEquals(List.of("12 NOT_PUBLISHED", "11 NOT_PUBLISHED", "10 NOT_PUBLISHED", "9 PUBLISHED", "8 PUBLISHED"),
                describe(first));
        assertEquals(List.of("2 PUBLISHED", "1 PUBLISHED"), describe(service.pageForAdmin(2026, null, null, 2, 5)));
        assertEquals(List.of(), service.pageForAdmin(2026, null, null, 9, 5).items());
    }

    @Test
    void theAdminStatusFilterSplitsAYearAtTheCurrentMonth() {
        assertEquals(9, service.pageForAdmin(2026, null, PUBLISHED, 0, 12).total());
        assertEquals(List.of("12 NOT_PUBLISHED", "11 NOT_PUBLISHED", "10 NOT_PUBLISHED"),
                describe(service.pageForAdmin(2026, null, NOT_PUBLISHED, 0, 12)));
        assertEquals(0, service.pageForAdmin(2025, null, NOT_PUBLISHED, 0, 12).total());
        assertEquals(0, service.pageForAdmin(2027, null, PUBLISHED, 0, 12).total());
        assertEquals(List.of("1 NOT_PUBLISHED"), describe(service.pageForAdmin(2027, null, NOT_PUBLISHED, 0, 12)));
    }

    @Test
    void theAdminMonthFilterShowsOnlyThatMonthsUploads() throws IOException {
        assertEquals(List.of("10 NOT_PUBLISHED"), describe(service.pageForAdmin(2026, 10, null, 0, 12)));
        assertEquals(List.of(), service.pageForAdmin(2026, 10, PUBLISHED, 0, 12).items());
        assertEquals(List.of(), service.pageForAdmin(2025, 3, null, 0, 12).items());
        assertThrows(IllegalArgumentException.class, () -> service.pageForAdmin(2025, 13, null, 0, 12));

        // A row that has outlived its PDF is still listed, flagged so the admin can replace it.
        Files.delete(storage.resolve("2025").resolve("08").resolve("feed_world_english.pdf"));
        AdminPublicationRowDto august = service.pageForAdmin(2025, 8, null, 0, 12).items().get(0);
        assertFalse(august.publication().isPdfAvailable());
        assertEquals(PUBLISHED, august.status());
    }

    @Test
    void anUploadLeftUntitledIsCalledFeedWorld() throws IOException {
        when(pdfProcessing.process(any(), any())).thenReturn(new PdfProcessingService.PdfMetadata(12));
        when(repository.save(any(Publication.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("Feed World", service.uploadPublication(pdf(), 2028, 1, PublicationLanguage.Telugu, null).getTitle());
        assertEquals("Feed World", service.uploadPublication(pdf(), 2028, 2, PublicationLanguage.Telugu, "   ").getTitle());
        assertEquals("Rythu Special",
                service.uploadPublication(pdf(), 2028, 3, PublicationLanguage.Telugu, "  Rythu Special ").getTitle());
        assertThrows(IllegalArgumentException.class,
                () -> service.uploadPublication(pdf(), 2028, 4, PublicationLanguage.Telugu, "x".repeat(256)));
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "issue.pdf", "application/pdf", "%PDF".getBytes());
    }

    private static List<String> describe(PageDto<AdminPublicationRowDto> page) {
        return page.items().stream().map(PublicationServiceImplTest::describe).toList();
    }

    private static String describe(AdminPublicationRowDto row) {
        return row.publication().getMonth() + " " + row.status();
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
