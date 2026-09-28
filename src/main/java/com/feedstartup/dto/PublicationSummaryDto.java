package com.feedstartup.dto;

import com.feedstartup.model.Publication;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Lightweight shape for archive lists (a year's issues, search results, etc). Excludes pdfUrl -
 * list views only need the cover thumbnail.
 */
public class PublicationSummaryDto {

    private String id;
    private String title;
    private Integer year;
    private Integer month;
    private String monthName;
    private String language;
    private Integer pageCount;
    private String thumbnailUrl;
    private LocalDate publishedDate;

    public static PublicationSummaryDto from(String id, Publication publication) {
        PublicationSummaryDto dto = new PublicationSummaryDto();
        dto.id = id;
        dto.title = publication.getTitle();
        dto.year = publication.getYear();
        dto.month = publication.getMonth();
        dto.monthName = Month.of(publication.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        dto.language = publication.getLanguage().name();
        dto.pageCount = publication.getPageCount();
        dto.thumbnailUrl = "/api/publications/" + id + "/thumbnail";
        dto.publishedDate = publication.getPublishedDate();
        return dto;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public Integer getYear() { return year; }
    public Integer getMonth() { return month; }
    public String getMonthName() { return monthName; }
    public String getLanguage() { return language; }
    public Integer getPageCount() { return pageCount; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public LocalDate getPublishedDate() { return publishedDate; }
}
