package com.feedstartup.dto;

import com.feedstartup.model.Publication;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/** Full detail shape used once a specific issue is opened in the viewer. */
public class PublicationDetailDto {

    private Long id;
    private String title;
    private Integer year;
    private Integer month;
    private String monthName;
    private Integer volume;
    private Integer issueNumber;
    private Integer pageCount;
    private LocalDate publishedDate;
    private String thumbnailUrl;
    private String pdfUrl;
    private Long fileSizeBytes;

    public static PublicationDetailDto from(Publication p) {
        PublicationDetailDto dto = new PublicationDetailDto();
        dto.id = p.getId();
        dto.title = p.getTitle();
        dto.year = p.getYear();
        dto.month = p.getMonth();
        dto.monthName = Month.of(p.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        dto.volume = p.getVolume();
        dto.issueNumber = p.getIssueNumber();
        dto.pageCount = p.getPageCount();
        dto.publishedDate = p.getPublishedDate();
        dto.thumbnailUrl = "/api/publications/" + p.getId() + "/thumbnail";
        dto.pdfUrl = "/api/publications/" + p.getId() + "/file";
        dto.fileSizeBytes = p.getFileSizeBytes();
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Integer getYear() { return year; }
    public Integer getMonth() { return month; }
    public String getMonthName() { return monthName; }
    public Integer getVolume() { return volume; }
    public Integer getIssueNumber() { return issueNumber; }
    public Integer getPageCount() { return pageCount; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getPdfUrl() { return pdfUrl; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
}
