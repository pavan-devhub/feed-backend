package com.feedstartup.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * The fixed set of photo sections on the EPM gallery page. The enum's id doubles as the on-disk
 * folder name under {@code epm.storage.gallery-dir}/{@code <id>}/ - see EpmGalleryServiceImpl.
 * Dropping image files straight into one of these folders (no upload API call needed) is enough
 * for them to show up in that section of the gallery page.
 */
public enum EpmGalleryBlock {
    EPM_GALLERY("epm-gallery", "EPM Gallery"),
    EPM_MOMENTS("epm-moments", "EPM Moments"),
    EPM_ACROSS_CITIES("epm-across-cities", "EPM Across Cities"),
    INSIDE_THE_EPM("inside-the-epm", "Inside the EPM"),
    PEOPLE_AT_EPM("people-at-epm", "People at EPM"),
    CONNECTIONS_AT_EPM("connections-at-epm", "Connections at EPM"),
    EVENT_DETAILS("event-details", "Event Details"),
    THE_EPM_EXPERIENCE("the-epm-experience", "The EPM Experience");

    private final String id;
    private final String label;

    EpmGalleryBlock(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }

    public static Optional<EpmGalleryBlock> fromId(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String trimmed = raw.trim();
        return Arrays.stream(values()).filter(b -> b.id.equalsIgnoreCase(trimmed)).findFirst();
    }

    public static String allowedIdsJoined() {
        return Arrays.stream(values()).map(EpmGalleryBlock::getId).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
