package com.feedstartup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One Feed World issue, in one language. Every month gets up to three rows - English, Telugu and
 * Hindi are published as separate PDFs, not one PDF with three translations - so the natural key
 * is (year, month, language), not just (year, month). The PDF and its cover thumbnail are still
 * written to disk under {@code storage/publications/<year>/<month>/} (see
 * PublicationServiceImpl), shared by all three languages' files for that month; this row - not a
 * meta.json sidecar - is the catalog, and {@link #pdfFile}/{@link #thumbnailFile} are the
 * filenames resolved against that same folder when a PDF or thumbnail is actually fetched.
 */
@Entity
@Table(name = "publications", uniqueConstraints = @UniqueConstraint(
        name = "uk_publications_year_month_language", columnNames = {"year", "month", "language"}))
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    // DEFAULT 'English' matters for schema *migration*, not just new rows: on an existing database
    // (ddl-auto=update) this lets Hibernate add the column to already-populated tables safely -
    // every publication predating this field was this catalog's only language, so backfilling it
    // as English is the one non-destructive reading of that old data (see PublicationServiceImpl).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'English'")
    private PublicationLanguage language = PublicationLanguage.English;

    @Column(nullable = false)
    private String title = "Feed World";

    private Integer volume;

    private Integer issueNumber;

    private Integer pageCount;

    private LocalDate publishedDate;

    // Filename only (random UUID + extension), resolved against this issue's own year/month folder.
    @Column(nullable = false)
    private String pdfFile;

    private String thumbnailFile;

    private Long fileSizeBytes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Publication() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public PublicationLanguage getLanguage() { return language; }
    public void setLanguage(PublicationLanguage language) { this.language = language; }

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
