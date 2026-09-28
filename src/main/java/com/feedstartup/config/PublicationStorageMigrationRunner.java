package com.feedstartup.config;

import com.feedstartup.model.Publication;
import com.feedstartup.repository.PublicationRepository;
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

/**
 * One-time, idempotent fixup for publications written under the pre-language storage layout
 * ({@code storage/publications/<year>/<month>/}) so they live where
 * {@code PublicationServiceImpl#buildPublicationDir} now looks for them
 * ({@code storage/publications/<year>/<language>/<month>/}). Every row already carries the
 * language that layout change needs (existing rows defaulted to English - the only language this
 * catalog supported before now), so this only moves files into place; it never guesses a
 * language or deletes a source file it hasn't first confirmed was moved. Runs on every startup
 * but is a no-op once a file is already at its new location, so it is safe to leave in place
 * rather than requiring a separate one-off script.
 */
@Component
public class PublicationStorageMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PublicationStorageMigrationRunner.class);

    private final PublicationRepository publicationRepository;

    @Value("${feedworld.storage.base-dir}")
    private String baseDir;

    public PublicationStorageMigrationRunner(PublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    @Override
    public void run(String... args) {
        List<Publication> all = publicationRepository.findAll();
        int filesMoved = 0;
        for (Publication publication : all) {
            Path legacyDir = Paths.get(baseDir, String.valueOf(publication.getYear()), String.format("%02d", publication.getMonth()));
            Path newDir = Paths.get(baseDir, String.valueOf(publication.getYear()),
                    publication.getLanguage().name().toLowerCase(), String.format("%02d", publication.getMonth()));

            if (moveIfNeeded(legacyDir, newDir, publication.getPdfFile())) filesMoved++;
            if (moveIfNeeded(legacyDir, newDir, publication.getThumbnailFile())) filesMoved++;

            // Best-effort: only succeeds once the folder has no files left in it (siblings in
            // other languages never shared this folder, so this never removes anything live).
            try {
                Files.deleteIfExists(legacyDir);
            } catch (IOException ignored) {
                // Still has files (migration failed, or another edition landed here pre-fix) - leave it.
            }
        }
        if (filesMoved > 0) {
            log.info("Publication storage migration: moved {} file(s) from the legacy <year>/<month> layout "
                    + "to <year>/<language>/<month>.", filesMoved);
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
}
