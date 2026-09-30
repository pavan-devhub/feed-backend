package com.feedstartup.dto;

import com.feedstartup.model.Publication;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/** Full detail shape used once a specific issue is opened in the viewer. */
public class PublicationDetailDto {

    private String id;
    private String title;
    private Integer year;
    private Integer month;
    private String monthName;
    private String language;
    private Integer order;
    private Integer pageCount;
    private LocalDate publishedDate;
    private String thumbnailUrl;
    private String pdfUrl;
    private boolean pdfAvailable;

    /** {@code pdfAvailable}: whether this row's PDF is actually on disk - see PublicationSummaryDto. */
    public static PublicationDetailDto from(String id, Publication publication, boolean pdfAvailable) {
        PublicationDetailDto dto = new PublicationDetailDto();
        dto.id = id;
        dto.title = publication.getTitle();
        dto.year = publication.getYear();
        dto.month = publication.getMonth();
        dto.monthName = Month.of(publication.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        dto.language = publication.getLanguage().name();
        dto.order = publication.getOrder();
        dto.pageCount = publication.getPageCount();
        dto.publishedDate = publication.getPublishedDate();
        dto.thumbnailUrl = "/api/publications/" + id + "/thumbnail";
        dto.pdfUrl = "/api/publications/" + id + "/file";
        dto.pdfAvailable = pdfAvailable;
        return dto;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public Integer getYear() { return year; }
    public Integer getMonth() { return month; }
    public String getMonthName() { return monthName; }
    public String getLanguage() { return language; }
    public Integer getOrder() { return order; }
    public Integer getPageCount() { return pageCount; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getPdfUrl() { return pdfUrl; }
    public boolean isPdfAvailable() { return pdfAvailable; }
}
