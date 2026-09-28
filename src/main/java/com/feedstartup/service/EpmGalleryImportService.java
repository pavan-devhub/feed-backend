package com.feedstartup.service;

/**
 * Registers image files that are on disk but have no database row yet - the photos that predate
 * the gallery tables, or ones copied straight into a gallery folder. Runs once automatically at
 * startup (see EpmDataBootstrapRunner) and on demand from the admin panel.
 */
public interface EpmGalleryImportService {

    record ImportResult(int imagesImported, int statesCreated, int districtsCreated, int sidecarsRemoved) {}

    ImportResult importFromStorage();
}
