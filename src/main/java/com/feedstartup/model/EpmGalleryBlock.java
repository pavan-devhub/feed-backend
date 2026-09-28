package com.feedstartup.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * The fixed set of image sections the admin can manage, across the EPM landing page and the EPM
 * gallery page. Each image's row in {@code epm_gallery_images} names its block; the file itself
 * lives on disk under {@code epm.storage.gallery-dir}/{@code <id>}/ - see EpmGalleryServiceImpl.
 * <p>
 * {@link #EPM_GALLERY} is also the root of the state -> district -> photo tree behind the gallery
 * page's state cards - see EpmGalleryRegionService. Only its loose photos (not the district ones)
 * belong to the block itself.
 */
public enum EpmGalleryBlock {
    // --- EPM landing page ---
    EPM_HERO("epm-hero", "Hero banner", Page.EPM_PAGE, 1,
            "The large banner at the top of the EPM page."),
    EPM_STATS("epm-stats", "Overview statistics", Page.EPM_PAGE, 3,
            "Background pictures of the three stat cards, in order: EPMs conducted, districts covered, total attendees."),
    EPM_CALENDAR("epm-calendar", "Calendar illustration", Page.EPM_PAGE, 1,
            "The picture under the Upcoming EPMs calendar."),
    EPM_CAROUSEL("epm-carousel", "Gallery carousel", Page.EPM_PAGE, 12,
            "Photos cycling in the EPM page's Gallery card. When empty, the first photos from the gallery page are shown."),

    // --- EPM gallery page ---
    EPM_GALLERY("epm-gallery", "Gallery intro", Page.GALLERY_PAGE, null,
            "The first three photos appear beside the gallery page's introduction."),
    EPM_MOMENTS("epm-moments", "EPM Moments", Page.GALLERY_PAGE, null, null),
    EPM_ACROSS_CITIES("epm-across-cities", "EPM Across Cities", Page.GALLERY_PAGE, null, null),
    INSIDE_THE_EPM("inside-the-epm", "Inside the EPM", Page.GALLERY_PAGE, null, null),
    PEOPLE_AT_EPM("people-at-epm", "People at EPM", Page.GALLERY_PAGE, null, null),
    CONNECTIONS_AT_EPM("connections-at-epm", "Connections at EPM", Page.GALLERY_PAGE, null, null),
    EVENT_DETAILS("event-details", "Event Details", Page.GALLERY_PAGE, null, null),
    THE_EPM_EXPERIENCE("the-epm-experience", "The EPM Experience", Page.GALLERY_PAGE, null, null);

    /** Which public page a block's images appear on. */
    public enum Page { EPM_PAGE, GALLERY_PAGE }

    private final String id;
    private final String label;
    private final Page page;
    // null = no limit
    private final Integer maxImages;
    private final String description;

    EpmGalleryBlock(String id, String label, Page page, Integer maxImages, String description) {
        this.id = id;
        this.label = label;
        this.page = page;
        this.maxImages = maxImages;
        this.description = description;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public Page getPage() { return page; }
    public Integer getMaxImages() { return maxImages; }
    public String getDescription() { return description; }

    public static Optional<EpmGalleryBlock> fromId(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String trimmed = raw.trim();
        return Arrays.stream(values()).filter(b -> b.id.equalsIgnoreCase(trimmed)).findFirst();
    }

    public static String allowedIdsJoined() {
        return Arrays.stream(values()).map(EpmGalleryBlock::getId).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
