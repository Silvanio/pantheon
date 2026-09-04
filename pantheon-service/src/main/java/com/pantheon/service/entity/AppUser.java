package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_subject")
    private String googleSubject;

    /**
     * Registration lifecycle. Nullable so accounts created before this column existed
     * remain valid; {@link #getRegistrationStatus()} reads a null column as
     * {@link RegistrationStatus#ACTIVE}. A {@code PENDING_REGISTRATION} account is a
     * placeholder created for an invited email and has no usable credentials.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status")
    private RegistrationStatus registrationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
        // JPA
    }

    public AppUser(UUID id, String email, String displayName, String passwordHash, String googleSubject, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.googleSubject = googleSubject;
        this.registrationStatus = RegistrationStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Placeholder account for an invited email that has no account yet: no credentials,
     * a display name derived from the email local-part, {@code PENDING_REGISTRATION}.
     */
    public static AppUser preRegistration(UUID id, String email, Instant now) {
        AppUser user = new AppUser(id, email, displayNameFromEmail(email), null, null, now, now);
        user.registrationStatus = RegistrationStatus.PENDING_REGISTRATION;
        return user;
    }

    private static String displayNameFromEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    /**
     * Completes a {@code PENDING_REGISTRATION} account with real credentials, making it a
     * normal, log-in-capable account.
     */
    public void completeRegistration(String passwordHash, String displayName) {
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.registrationStatus = RegistrationStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void linkGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    /** Reads a null column (legacy accounts) as {@link RegistrationStatus#ACTIVE}. */
    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus != null ? registrationStatus : RegistrationStatus.ACTIVE;
    }

    public boolean isPendingRegistration() {
        return getRegistrationStatus() == RegistrationStatus.PENDING_REGISTRATION;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
