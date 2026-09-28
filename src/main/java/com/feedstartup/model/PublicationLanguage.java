package com.feedstartup.model;

/**
 * A Feed World issue is published separately in each of these languages - one PDF per
 * (year, month, language), not one PDF per month. See {@link Publication}'s unique constraint.
 * Constant names match the exact strings already stored in the `language` column
 * (feed_db.publications) so {@code @Enumerated(EnumType.STRING)} round-trips without a converter.
 */
public enum PublicationLanguage {
    English,
    Telugu,
    Hindi
}
