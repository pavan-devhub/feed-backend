package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A participant's submission of the "Register for EPM" form. The event's city/state/date are
 * copied in (not just referenced by {@link #epmEventId}) so this record still reads correctly after
 * that EpmEvent is removed; while the event exists, the copy follows it when it is rescheduled or
 * moved (see EpmEventServiceImpl#update).
 */
@Entity
@Table(name = "epm_registrations")
public class EpmRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "epm_event_id", nullable = false)
    private Long epmEventId;

    // The account this sign-up belongs to: the one logged in when the (public) form was sent, or
    // for a signed-out one, the account with the same email or mobile number - if any. A foreign
    // key to users (ON DELETE SET NULL - the sign-up outlives the account), added by
    // UserLinksMigrationRunner.
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_city", nullable = false)
    private String eventCity;

    @Column(name = "event_state", nullable = false)
    private String eventState;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    private String email;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String district;

    // One of the user_types rows the EPM forms offer (UserType#epmOrder) - a foreign key. Nullable
    // only so Hibernate can add the column to a table that already has rows; UserLinksMigrationRunner
    // fills it in and makes it NOT NULL. (The form's old choices - Farmer, FPO... - were plain text;
    // the runner kept them as legacy_participant_type.)
    @ManyToOne
    @JoinColumn(name = "participant_type_id", foreignKey = @ForeignKey(name = "fk_epm_registrations_participant_type"))
    private UserType participantType;

    @Column(nullable = false)
    private boolean consent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public EpmRegistration() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getEpmEventId() { return epmEventId; }
    public void setEpmEventId(Long epmEventId) { this.epmEventId = epmEventId; }

    public String getEventCity() { return eventCity; }
    public void setEventCity(String eventCity) { this.eventCity = eventCity; }

    public String getEventState() { return eventState; }
    public void setEventState(String eventState) { this.eventState = eventState; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public UserType getParticipantType() { return participantType; }
    public void setParticipantType(UserType participantType) { this.participantType = participantType; }

    public boolean isConsent() { return consent; }
    public void setConsent(boolean consent) { this.consent = consent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
