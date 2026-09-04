package com.feedstartup.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * The fixed set of EPM event categories. This is the single source of truth for what a
 * category is allowed to be - both the admin create/update flow (EpmEventServiceImpl) and the
 * public category filter (EpmEventController#categories) read off this enum, so adding or
 * renaming a category only ever requires a change here.
 */
public enum EpmCategory {
    GAP_WORKSHOP("GAP Workshop", "Good Agricultural Practices (GAP Workshop)"),
    CAPACITY_BUILDING_TRAININGS("Capacity Building Trainings", "Capacity Building Trainings"),
    FPO_MANAGEMENT_SESSIONS("FPO Management Sessions", "FPO Management Sessions"),
    EPM_MEETING("EPM Meeting", "EPM Meeting"),
    EXPORT_WORKSHOPS("Export Workshops", "Export Workshops");

    // Canonical value stored on EpmEvent#category and used for filtering.
    private final String id;
    // Longer display label shown in the UI.
    private final String label;

    EpmCategory(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }

    public static Optional<EpmCategory> fromId(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String trimmed = raw.trim();
        return Arrays.stream(values()).filter(c -> c.id.equalsIgnoreCase(trimmed)).findFirst();
    }

    public static String allowedIdsJoined() {
        return Arrays.stream(values()).map(EpmCategory::getId).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
