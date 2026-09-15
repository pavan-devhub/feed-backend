package com.feedstartup.dto;

import com.feedstartup.model.PublicationMeta;

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
    private Integer volume;
    private Integer issueNumber;
    private Integer pageCount;
    private LocalDate publishedDate;
    private String thumbnailUrl;
    private String pdfUrl;
    private Long fileSizeBytes;

    public static PublicationDetailDto from(String id, Integer year, Integer month, PublicationMeta meta) {
        PublicationDetailDto dto = new PublicationDetailDto();
        dto.id = id;
        dto.title = meta.getTitle();
        dto.year = year;
        dto.month = month;
        dto.monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        dto.volume = meta.getVolume();
        dto.issueNumber = meta.getIssueNumber();
        dto.pageCount = meta.getPageCount();
        dto.publishedDate = meta.getPublishedDate();
        dto.thumbnailUrl = "/api/publications/" + id + "/thumbnail";
        dto.pdfUrl = "/api/publications/" + id + "/file";
        dto.fileSizeBytes = meta.getFileSizeBytes();
        return dto;
    }

    public String getId() { return id; }
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
