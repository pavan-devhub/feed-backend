package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An EPM event category, managed by the admin. {@link #name} is the canonical value stored on
 * EpmEvent#category and matched by the public category filter, so renaming a category rewrites it
 * on every event too (see EpmCategoryServiceImpl). {@link #label} is the longer text shown in the
 * "Filter by Category" sidebar.
 */
@Entity
@Table(name = "epm_categories")
public class EpmCategory {

    /** Accent colours the frontend has styles for (see EpmDetails.css color-* / tag-*). */
    public static final List<String> COLORS = List.of("green", "teal", "purple", "blue", "orange", "emerald", "gray");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false, length = 20)
    private String color = "gray";

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

    public EpmCategory() {}

    public EpmCategory(String name, String label, String color, int displayOrder) {
        this.name = name;
        this.label = label;
        this.color = color;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
