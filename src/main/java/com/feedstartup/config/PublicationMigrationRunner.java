package com.feedstartup.config;

import com.feedstartup.model.Publication;
import com.feedstartup.model.PublicationLanguage;
import com.feedstartup.repository.PublicationRepository;
import com.feedstartup.service.PdfProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/**
 * Idempotent startup fixups for the publications catalog. Every step runs on every startup but is a
 * no-op once everything is in place, so this is safe to leave in rather than requiring a separate
 * one-off script.
 * <ol>
 *   <li>The `order` column: rows that predate it were all added with the column's DEFAULT
 *       (English's 3), so each non-English row is corrected to its language's value
 *       (see PublicationLanguage#getOrder).</li>
 *   <li>Storage layout: files written under the earlier
 *       {@code storage/publications/<year>/<language>/<month>/} layout are moved up to
 *       {@code storage/publications/<year>/<month>/}, where
 *       {@code PublicationServiceImpl#buildPublicationDir} now looks for them, and the emptied
 *       language folders are removed. This never deletes a file - only folders that are already
 *       empty.</li>
 *   <li>Filenames: files stored under the earlier random UUID names are renamed to
 *       {@code feed_world_<language>.<ext>} (see PublicationLanguage#storedFileBaseName) and the
 *       row's pdf_file / thumbnail_file updated to match. If a rename succeeded but saving the row
 *       didn't, the next startup sees the file already under its new name and just fixes the row.</li>
 *   <li>PNG covers: a cover thumbnail is always {@code feed_world_<language>.png} (see
 *       PublicationLanguage#thumbnailFileName). A row whose thumbnail_file names another format
 *       (older uploads kept .avif covers), names nothing, or names a PNG that has gone missing
 *       gets a fresh PNG rendered from its PDF - Java can't decode AVIF, and the PDF is the
 *       cover's source anyway. The row is saved with the PNG's name before the old file is
 *       deleted, so a failure part-way through never leaves a row pointing at a missing file.</li>
 * </ol>
 */
@Component
public class PublicationMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PublicationMigrationRunner.class);

    /** Formats older versions stored covers in - removed once the edition's PNG exists. */
    private static final List<String> LEGACY_THUMBNAIL_EXTENSIONS = List.of(".avif", ".webp", ".jpg", ".jpeg");

    private final PublicationRepository publicationRepository;
    private final PdfProcessingService pdfProcessingService;

    @Value("${feedworld.storage.base-dir}")
    private String baseDir;

    public PublicationMigrationRunner(PublicationRepository publicationRepository, PdfProcessingService pdfProcessingService) {
        this.publicationRepository = publicationRepository;
        this.pdfProcessingService = pdfProcessingService;
    }

    @Override
    public void run(String... args) {
        syncLanguageOrder();
        moveFilesOutOfLanguageFolders();
        renameFilesByLanguage();
        convertThumbnailsToPng();
    }

    private void convertThumbnailsToPng() {
        int converted = 0;
        for (Publication publication : publicationRepository.findAll()) {
            Path monthDir = Paths.get(baseDir, String.valueOf(publication.getYear()),
                    String.format("%02d", publication.getMonth()));
            PublicationLanguage language = publication.getLanguage();
            String pngName = language.thumbnailFileName();
            Path png = monthDir.resolve(pngName);
            String previous = publication.getThumbnailFile();

            if (!pngName.equals(previous) || !Files.exists(png)) {
                Path pdf = publication.getPdfFile() == null ? null : monthDir.resolve(publication.getPdfFile());
                if (pdf == null || !Files.exists(pdf)) {
                    // Nothing to render from - left alone; the admin dashboard shows it as not uploaded.
                    continue;
                }
                try {
                    PdfProcessingService.PdfMetadata metadata = pdfProcessingService.process(pdf, png);
                    if (publication.getPageCount() == null) {
                        publication.setPageCount(metadata.pageCount());
                    }
                } catch (IOException | RuntimeException e) {
                    log.warn("Could not render a PNG cover for {}: {}", pdf, e.getMessage());
                    continue;
                }
                if (!Files.exists(png)) {
                    continue; // a PDF with no pages has no cover to render
                }
                publication.setThumbnailFile(pngName);
                publicationRepository.save(publication);
                converted++;
                if (previous != null && !previous.equals(pngName)) {
                    deleteFileQuietly(monthDir.resolve(previous));
                }
            }

            // The PNG is in place - sweep away any cover this edition still has in an older format.
            for (String extension : LEGACY_THUMBNAIL_EXTENSIONS) {
                deleteFileQuietly(monthDir.resolve(language.storedFileBaseName() + extension));
            }
        }
        if (converted > 0) {
            log.info("Publication migration: rendered PNG covers for {} publication(s) and removed their old-format covers.", converted);
        }
    }

    private static void deleteFileQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete old publication cover {}: {}", file, e.getMessage());
        }
    }

    private void syncLanguageOrder() {
        int rowsUpdated = 0;
        for (PublicationLanguage language : PublicationLanguage.values()) {
            rowsUpdated += publicationRepository.syncOrder(language, language.getOrder());
        }
        if (rowsUpdated > 0) {
            log.info("Publication migration: set the `order` column on {} row(s) to match their language.", rowsUpdated);
        }
    }

    private void moveFilesOutOfLanguageFolders() {
        List<Publication> all = publicationRepository.findAll();
        int filesMoved = 0;
        for (Publication publication : all) {
            Path yearDir = Paths.get(baseDir, String.valueOf(publication.getYear()));
            String month = String.format("%02d", publication.getMonth());
            Path languageDir = yearDir.resolve(publication.getLanguage().name().toLowerCase());
            Path legacyDir = languageDir.resolve(month);
            Path newDir = yearDir.resolve(month);

            if (moveIfNeeded(legacyDir, newDir, publication.getPdfFile())) filesMoved++;
            if (moveIfNeeded(legacyDir, newDir, publication.getThumbnailFile())) filesMoved++;

            // Best-effort: each delete only succeeds once that folder is empty, so a language
            // folder is removed after its last month has been moved out, and a folder still
            // holding a file this loop didn't recognise is left alone.
            deleteIfEmpty(legacyDir);
            deleteIfEmpty(languageDir);
        }
        if (filesMoved > 0) {
            log.info("Publication migration: moved {} file(s) from the <year>/<language>/<month> layout "
                    + "to <year>/<month>.", filesMoved);
        }
    }

    private void renameFilesByLanguage() {
        int rowsUpdated = 0;
        for (Publication publication : publicationRepository.findAll()) {
            Path monthDir = Paths.get(baseDir, String.valueOf(publication.getYear()),
                    String.format("%02d", publication.getMonth()));
            String baseName = publication.getLanguage().storedFileBaseName();

            String pdfFile = renameIfNeeded(monthDir, publication.getPdfFile(), baseName);
            String thumbnailFile = renameIfNeeded(monthDir, publication.getThumbnailFile(), baseName);
            if (!Objects.equals(pdfFile, publication.getPdfFile())
                    || !Objects.equals(thumbnailFile, publication.getThumbnailFile())) {
                publication.setPdfFile(pdfFile);
                publication.setThumbnailFile(thumbnailFile);
                publicationRepository.save(publication);
                rowsUpdated++;
            }
        }
        if (rowsUpdated > 0) {
            log.info("Publication migration: renamed the files of {} publication(s) to feed_world_<language>.", rowsUpdated);
        }
    }

    /**
     * Returns the name {@code filename} should be recorded under: {@code baseName} + its original
     * extension once the file is (or already was) stored under that name, otherwise
     * {@code filename} unchanged - a file missing from disk, or a clash with an unrelated file
     * already holding the new name, is left for the admin rather than guessed at.
     */
    private String renameIfNeeded(Path dir, String filename, String baseName) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        String target = baseName + (dot >= 0 ? filename.substring(dot) : "");
        if (target.equals(filename)) {
            return filename;
        }
        Path current = dir.resolve(filename);
        Path renamed = dir.resolve(target);
        boolean currentExists = Files.exists(current);
        boolean renamedExists = Files.exists(renamed);
        if (!currentExists && renamedExists) {
            return target;
        }
        if (!currentExists || renamedExists) {
            if (renamedExists) {
                log.warn("Not renaming publication file {} - {} already exists", current, renamed);
            }
            return filename;
        }
        try {
            Files.move(current, renamed);
            return target;
        } catch (IOException e) {
            log.warn("Could not rename publication file {} to {}: {}", current, renamed, e.getMessage());
            return filename;
        }
    }

    private boolean moveIfNeeded(Path legacyDir, Path newDir, String filename) {
        if (filename == null) {
            return false;
        }
        Path legacyFile = legacyDir.resolve(filename);
        Path newFile = newDir.resolve(filename);
        if (Files.exists(newFile) || !Files.exists(legacyFile)) {
            return false;
        }
        try {
            Files.createDirectories(newDir);
            Files.move(legacyFile, newFile, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            log.warn("Could not migrate publication file {} to {}: {}", legacyFile, newFile, e.getMessage());
            return false;
        }
    }

    private static void deleteIfEmpty(Path dir) {
        try {
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
            // Not empty (yet) - leave it.
        }
    }
}
