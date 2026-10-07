package com.feedstartup.config;

import com.feedstartup.model.Publication;
import com.feedstartup.model.PublicationLanguage;
import com.feedstartup.repository.PublicationRepository;
import com.feedstartup.service.impl.PdfProcessingServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Every publication cover ends up as feed_world_<language>.png, in the month folder and in its row. */
class PublicationMigrationRunnerTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

    @TempDir
    Path storage;

    private final PublicationRepository repository = mock(PublicationRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final List<Publication> rows = new ArrayList<>();
    private PublicationMigrationRunner runner;

    @BeforeEach
    void setUp() {
        runner = new PublicationMigrationRunner(repository, new PdfProcessingServiceImpl(), jdbc);
        ReflectionTestUtils.setField(runner, "baseDir", storage.toString());
        when(repository.findAll()).thenReturn(rows);
        when(repository.syncOrder(any(), anyInt())).thenReturn(0);
    }

    @Test
    void theOldOneEditionPerMonthIndexIsDropped() {
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of("UKqxtw98ahdr1g9x6xwx0ck6qoh"));

        runner.dropOneEditionPerMonthIndex();

        verify(jdbc).execute("ALTER TABLE publications DROP INDEX `UKqxtw98ahdr1g9x6xwx0ck6qoh`");
    }

    @Test
    void nothingIsDroppedWhenOnlyTheLanguageIndexExists() {
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of());

        runner.dropOneEditionPerMonthIndex();

        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void aFailedIndexCheckDoesNotStopTheOtherSteps() {
        when(jdbc.queryForList(anyString(), eq(String.class))).thenThrow(new RuntimeException("no information_schema"));

        runner.run();

        verify(repository, atLeastOnce()).findAll();
    }

    @Test
    void anAvifCoverIsReRenderedAsPngAndTheAvifRemoved() throws IOException {
        Publication august = row(2025, 8, PublicationLanguage.English, "feed_world_english.avif");
        Path month = storage.resolve("2025/08");
        writePdf(month.resolve("feed_world_english.pdf"));
        Files.writeString(month.resolve("feed_world_english.avif"), "not really an avif");

        runner.run();

        assertEquals("feed_world_english.png", august.getThumbnailFile());
        assertIsPng(month.resolve("feed_world_english.png"));
        assertFalse(Files.exists(month.resolve("feed_world_english.avif")));
        verify(repository).save(august);
    }

    @Test
    void aPngRowIsLeftAloneButAStrayOldFormatCoverIsSweptAway() throws IOException {
        Publication april = row(2026, 4, PublicationLanguage.Telugu, "feed_world_telugu.png");
        Path month = storage.resolve("2026/04");
        writePdf(month.resolve("feed_world_telugu.pdf"));
        byte[] existingPng = Files.readAllBytes(renderPng(month.resolve("feed_world_telugu.pdf"), month.resolve("feed_world_telugu.png")));
        Files.writeString(month.resolve("feed_world_telugu.webp"), "leftover");

        runner.run();

        verify(repository, never()).save(april);
        assertArrayEquals(existingPng, Files.readAllBytes(month.resolve("feed_world_telugu.png")));
        assertFalse(Files.exists(month.resolve("feed_world_telugu.webp")));
    }

    @Test
    void aMissingPngIsRenderedAgainFromThePdf() throws IOException {
        Publication june = row(2028, 6, PublicationLanguage.Hindi, "feed_world_hindi.png");
        writePdf(storage.resolve("2028/06/feed_world_hindi.pdf"));

        runner.run();

        assertIsPng(storage.resolve("2028/06/feed_world_hindi.png"));
        verify(repository).save(june);
    }

    @Test
    void withoutAPdfThereIsNothingToRenderSoTheRowAndItsCoverAreKept() throws IOException {
        Publication march = row(2026, 3, PublicationLanguage.English, "feed_world_english.avif");
        Path avif = Files.createDirectories(storage.resolve("2026/03")).resolve("feed_world_english.avif");
        Files.writeString(avif, "cover");

        runner.run();

        assertEquals("feed_world_english.avif", march.getThumbnailFile());
        assertTrue(Files.exists(avif));
        verify(repository, never()).save(march);
    }

    @Test
    void runningAgainChangesNothing() throws IOException {
        row(2025, 9, PublicationLanguage.English, "feed_world_english.avif");
        Path month = storage.resolve("2025/09");
        writePdf(month.resolve("feed_world_english.pdf"));
        Files.writeString(month.resolve("feed_world_english.avif"), "old");

        runner.run();
        byte[] afterFirstRun = Files.readAllBytes(month.resolve("feed_world_english.png"));
        runner.run();

        verify(repository, times(1)).save(any());
        assertArrayEquals(afterFirstRun, Files.readAllBytes(month.resolve("feed_world_english.png")));
    }

    @Test
    void theRendererRefusesToWriteACoverInAnyFormatButPng() throws IOException {
        Path pdf = storage.resolve("issue.pdf");
        writePdf(pdf);

        assertThrows(IllegalArgumentException.class,
                () -> new PdfProcessingServiceImpl().process(pdf, storage.resolve("cover.avif")));
    }

    private Publication row(int year, int month, PublicationLanguage language, String thumbnailFile) {
        Publication publication = new Publication();
        publication.setYear(year);
        publication.setMonth(month);
        publication.setLanguage(language);
        publication.setPdfFile(language.pdfFileName());
        publication.setThumbnailFile(thumbnailFile);
        rows.add(publication);
        return publication;
    }

    private static void writePdf(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.save(path.toFile());
        }
    }

    private static Path renderPng(Path pdf, Path png) throws IOException {
        new PdfProcessingServiceImpl().process(pdf, png);
        return png;
    }

    private static void assertIsPng(Path file) throws IOException {
        assertTrue(Files.exists(file), file + " should exist");
        byte[] head = Arrays.copyOf(Files.readAllBytes(file), PNG_SIGNATURE.length);
        assertArrayEquals(PNG_SIGNATURE, head, file + " should be a real PNG");
    }
}
