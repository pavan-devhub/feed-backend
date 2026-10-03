package com.feedstartup.dto;

/**
 * One row of the admin dashboard's publication table: one uploaded edition and whether readers
 * can see it yet. {@code publication.pdfAvailable} is false for a row that has outlived its PDF.
 */
public record AdminPublicationRowDto(PublicationSummaryDto publication, Status status) {

    /**
     * PUBLISHED: the issue's month has begun, so readers can open it. NOT_PUBLISHED: it was
     * uploaded ahead of its month and goes live on the 1st (see PublicationVisibility).
     */
    public enum Status { PUBLISHED, NOT_PUBLISHED }
}
