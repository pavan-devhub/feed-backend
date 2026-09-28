package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Marks a one-time data step (e.g. seeding the default EPM categories) as done, so it never runs
 * again - even after the admin deletes everything it created. See EpmDataBootstrapRunner.
 */
@Entity
@Table(name = "data_migrations")
public class DataMigration {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    public DataMigration() {}

    public DataMigration(String id) {
        this.id = id;
        this.appliedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
