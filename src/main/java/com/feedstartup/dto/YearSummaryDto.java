package com.feedstartup.dto;

import java.util.List;

/**
 * Powers the year dropdowns and collapsible year groups on both the reader's publications page
 * and the admin dashboard. {@code count} is every row filed under the year (what the admin
 * manages); {@code languages} only counts editions whose PDF is actually on disk - what a reader
 * can open - so the reader page never advertises an issue it can't show.
 */
public class YearSummaryDto {

    /** Readable editions of one language within the year, e.g. {"language": "Telugu", "count": 2}. */
    public record LanguageCount(String language, long count) {}

    private final Integer year;
    private final Long count;
    private final List<LanguageCount> languages;

    public YearSummaryDto(Integer year, Long count, List<LanguageCount> languages) {
        this.year = year;
        this.count = count;
        this.languages = languages;
    }

    public Integer getYear() { return year; }
    public Long getCount() { return count; }
    public List<LanguageCount> getLanguages() { return languages; }
}
