package com.feedstartup.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A kind of user someone registers as - Individual, Student, Government and so on - listed in the
 * {@code user_types} table. {@link #name} is the exact value stored on User#userType (and carried
 * in the JWT's userType claim), so it is never renamed; a type that should stop being offered is
 * switched off with {@link #active} instead, which keeps every existing user's value meaningful.
 * The defaults are seeded by UserTypeBootstrapRunner.
 *
 * <p>The same table is the list of EPM participant types: the types with an {@link #epmOrder} are
 * the ones the EPM register and volunteer forms offer, and every EPM registration / volunteer row
 * points at one of them with a foreign key (participant_type_id).
 */
@Entity
@Table(name = "user_types")
public class UserType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** Position in the registration form's "User Type" dropdown. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Whether new registrations may choose it. */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Position in the EPM forms' "Participant Type" dropdown, or null if EPM participants can't
     * choose it. Independent of {@link #active}, which is about creating accounts.
     */
    @Column(name = "epm_order")
    private Integer epmOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UserType() {}

    public UserType(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Integer getEpmOrder() { return epmOrder; }
    public void setEpmOrder(Integer epmOrder) { this.epmOrder = epmOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
