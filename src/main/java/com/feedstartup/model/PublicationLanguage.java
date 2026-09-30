package com.feedstartup.model;

/**
 * A Feed World issue is published separately in each of these languages - one PDF per
 * (year, month, language), not one PDF per month. See {@link Publication}'s unique constraint.
 * Constant names match the exact strings already stored in the `language` column
 * (feed_db.publications) so {@code @Enumerated(EnumType.STRING)} round-trips without a converter.
 * Declared in display order: {@link #getOrder()} is the value stored in each row's `order` column
 * (see {@link Publication}), which every catalog query sorts a month's editions by.
 */
public enum PublicationLanguage {
    Telugu(1),
    Hindi(2),
    English(3);

    private final int order;

    PublicationLanguage(int order) {
        this.order = order;
    }

    public int getOrder() { return order; }

    /**
     * Name (without extension) this edition's PDF and cover thumbnail are stored under inside
     * their month folder, e.g. "feed_world_telugu" -> feed_world_telugu.pdf / feed_world_telugu.png.
     * A month holds at most one edition per language, so the language alone keeps the three
     * editions' files apart.
     */
    public String storedFileBaseName() {
        return "feed_world_" + name().toLowerCase();
    }

    /** e.g. "feed_world_telugu.pdf". */
    public String pdfFileName() {
        return storedFileBaseName() + ".pdf";
    }

    /**
     * e.g. "feed_world_telugu.png". A cover thumbnail is always a PNG rendered from the PDF's first
     * page (see PdfProcessingService) - never AVIF, WebP or JPEG. Covers stored in another format
     * by older versions are converted on startup by PublicationMigrationRunner.
     */
    public String thumbnailFileName() {
        return storedFileBaseName() + ".png";
    }
}
