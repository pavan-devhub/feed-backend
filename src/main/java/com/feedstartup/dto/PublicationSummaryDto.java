package com.feedstartup.dto;

import com.feedstartup.model.Publication;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Lightweight shape used for archive lists (a whole year's worth of issues, search results, etc.)
 * Deliberately excludes the pdfUrl - the archive list only needs the cover thumbnail until a
 * specific issue is opened, which keeps the payload small when a year has many issues.
 */
public class PublicationSummaryDto {

    private Long id;
    private String title;
    private Integer year;
    private Integer month;
    private String monthName;
    private Integer pageCount;
    private String thumbnailUrl;
    private LocalDate publishedDate;

    public static PublicationSummaryDto from(Publication p) {
        PublicationSummaryDto dto = new PublicationSummaryDto();
        dto.id = p.getId();
        dto.title = p.getTitle();
        dto.year = p.getYear();
        dto.month = p.getMonth();
        dto.monthName = Month.of(p.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        dto.pageCount = p.getPageCount();
        dto.thumbnailUrl = "/api/publications/" + p.getId() + "/thumbnail";
        dto.publishedDate = p.getPublishedDate();
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Integer getYear() { return year; }
    public Integer getMonth() { return month; }
    public String getMonthName() { return monthName; }
    public Integer getPageCount() { return pageCount; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public LocalDate getPublishedDate() { return publishedDate; }
}
