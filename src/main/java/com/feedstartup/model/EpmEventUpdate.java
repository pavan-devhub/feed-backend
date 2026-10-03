package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One change an admin made to an EpmEvent after it was scheduled - a new date, time, place or
 * description, or the EPM being cancelled / reinstated. Kept as a log so the admin table can say
 * what changed and the people registered or volunteering can see each update on their dashboard.
 */
@Entity
@Table(name = "epm_event_updates", indexes = @Index(name = "idx_epm_event_updates_event", columnList = "epm_event_id"))
public class EpmEventUpdate {

    public enum Field { DATE, TIME, STATE, DISTRICT, CITY, VENUE, DESCRIPTION, CANCELLED, RESTORED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "epm_event_id", nullable = false)
    private Long epmEventId;

    // A plain varchar rather than a MySQL ENUM column, so adding a Field later needs no migration.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private Field field;

    // Both values are the display text ("2026-10-15", "10:00 AM - 02:00 PM", a venue name...);
    // null means "not set". For CANCELLED, newValue holds the admin's optional reason.
    @Lob
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Lob
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public EpmEventUpdate() {}

    public EpmEventUpdate(Long epmEventId, Field field, String oldValue, String newValue) {
        this.epmEventId = epmEventId;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEpmEventId() { return epmEventId; }
    public void setEpmEventId(Long epmEventId) { this.epmEventId = epmEventId; }

    public Field getField() { return field; }
    public void setField(Field field) { this.field = field; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
