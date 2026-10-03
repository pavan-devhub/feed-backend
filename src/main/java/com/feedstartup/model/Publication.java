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

    /** The title an issue gets when the admin leaves the upload form's title blank. */
    public static final String TITLE = "Feed World";

    public static final int TITLE_MAX_LENGTH = 255;

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

    // Telugu = 1, Hindi = 2, English = 3 - always derived from `language` (see
    // PublicationLanguage#getOrder), never set on its own; stored as a real column so catalog
    // queries can ORDER BY it (see PublicationRepository#CATALOG_ORDER). `order` is a reserved
    // word in MySQL, hence the backticks - raw SQL against this table has to quote it as well.
    // DEFAULT 3 plays the same role as language's DEFAULT 'English' above: it lets ddl-auto add
    // the column to an already-populated table; PublicationMigrationRunner then corrects any
    // non-English rows on startup.
    @Column(name = "`order`", nullable = false)
    @ColumnDefault("3")
    private Integer order = PublicationLanguage.English.getOrder();

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title = TITLE;

    private Integer pageCount;

    private LocalDate publishedDate;

    // Filename only (feed_world_<language> + extension, see PublicationLanguage#storedFileBaseName),
    // resolved against this issue's own year/month folder.
    @Column(nullable = false)
    private String pdfFile;

    private String thumbnailFile;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        order = language.getOrder();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        order = language.getOrder();
    }

    public Publication() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public PublicationLanguage getLanguage() { return language; }
    public void setLanguage(PublicationLanguage language) {
        this.language = language;
        this.order = language.getOrder();
    }

    public Integer getOrder() { return order; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }

    public String getPdfFile() { return pdfFile; }
    public void setPdfFile(String pdfFile) { this.pdfFile = pdfFile; }

    public String getThumbnailFile() { return thumbnailFile; }
    public void setThumbnailFile(String thumbnailFile) { this.thumbnailFile = thumbnailFile; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
