package com.feedstartup.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One thing an admin did to an EpmEvent - scheduled it, changed one of its details, cancelled,
 * reinstated or deleted it - and who did it, for the admin panel's "EPM Activity" log.
 *
 * <p>The log is append-only: rows are written once and never changed or removed (@Immutable, no
 * setters, and EpmAdminActivityRepository has no update or delete). Unlike EpmEventUpdate - the
 * change log people signed up to an EPM see, which goes when the EPM is deleted - nothing here
 * points at another table: the admin's username and the EPM's title, date and place are copied in,
 * so an entry still reads the same after its EPM or admin is deleted.
 */
@Entity
@Immutable
@Table(name = "epm_admin_activities", indexes = {
        @Index(name = "idx_epm_admin_activities_created", columnList = "created_at"),
        @Index(name = "idx_epm_admin_activities_admin", columnList = "admin_id"),
        @Index(name = "idx_epm_admin_activities_event_date", columnList = "event_date")
})
public class EpmAdminActivity {

    public enum Action { CREATED, UPDATED, CANCELLED, REINSTATED, DELETED }

    /** What an UPDATED entry changed - every field on the admin's EPM form, in the order they're logged. */
    public enum Field { TITLE, CATEGORY, DATE, TIME, STATE, DISTRICT, CITY, VENUE, DESCRIPTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false, updatable = false)
    private Long adminId;

    @Column(name = "admin_username", nullable = false, updatable = false, length = 30)
    private String adminUsername;

    // Plain varchars rather than MySQL ENUM columns, like EpmEventUpdate#field.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, columnDefinition = "varchar(20)")
    private Action action;

    // Set on UPDATED entries only.
    @Enumerated(EnumType.STRING)
    @Column(updatable = false, columnDefinition = "varchar(20)")
    private Field field;

    @Column(name = "epm_event_id", nullable = false, updatable = false)
    private Long epmEventId;

    // The EPM as it stood right after the action (just before it, for DELETED).
    @Column(name = "event_title", nullable = false, updatable = false)
    private String eventTitle;

    @Column(name = "event_date", nullable = false, updatable = false)
    private LocalDate eventDate;

    @Column(name = "event_city", updatable = false)
    private String eventCity;

    // An UPDATED field's value before and after, as stored on the EPM ("2026-10-15" for a date);
    // null means "not set". For CANCELLED, newValue holds the admin's optional reason.
    @Lob
    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Lob
    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    // Shared by every entry one save writes, so they list together.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected EpmAdminActivity() {}

    public EpmAdminActivity(LocalDateTime createdAt, Long adminId, String adminUsername, Action action, Field field,
                            EpmEvent event, String oldValue, String newValue) {
        this.createdAt = createdAt;
        this.adminId = adminId;
        this.adminUsername = adminUsername;
        this.action = action;
        this.field = field;
        this.epmEventId = event.getId();
        this.eventTitle = event.getTitle();
        this.eventDate = event.getEventDate();
        this.eventCity = event.getCity();
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getId() { return id; }
    public Long getAdminId() { return adminId; }
    public String getAdminUsername() { return adminUsername; }
    public Action getAction() { return action; }
    public Field getField() { return field; }
    public Long getEpmEventId() { return epmEventId; }
    public String getEventTitle() { return eventTitle; }
    public LocalDate getEventDate() { return eventDate; }
    public String getEventCity() { return eventCity; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
