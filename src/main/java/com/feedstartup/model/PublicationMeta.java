package com.feedstartup.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Metadata for one Feed World issue. Persisted as a JSON sidecar file ({@code meta.json}) inside
 * that issue's own {@code storage/publications/<year>/<month>/} folder, next to its PDF and
 * thumbnail - there is no database row for publications. The year and month come from the folder
 * path itself, not from this class.
 */
public class PublicationMeta {

    private String title = "Feed World";

    private Integer volume;

    private Integer issueNumber;

    private Integer pageCount;

    private LocalDate publishedDate;

    // Filenames only (random UUID + extension), resolved against this issue's own month folder.
    private String pdfFile;

    private String thumbnailFile;

    private Long fileSizeBytes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public PublicationMeta() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getVolume() { return volume; }
    public void setVolume(Integer volume) { this.volume = volume; }

    public Integer getIssueNumber() { return issueNumber; }
    public void setIssueNumber(Integer issueNumber) { this.issueNumber = issueNumber; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }

    public String getPdfFile() { return pdfFile; }
    public void setPdfFile(String pdfFile) { this.pdfFile = pdfFile; }

    public String getThumbnailFile() { return thumbnailFile; }
    public void setThumbnailFile(String thumbnailFile) { this.thumbnailFile = thumbnailFile; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
