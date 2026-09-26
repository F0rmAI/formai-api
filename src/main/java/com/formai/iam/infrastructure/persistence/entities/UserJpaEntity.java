package com.formai.iam.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "iam")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "hashed_password")
    private String hashedPassword;

    // Stored as plain strings, not the domain Role type: this entity stays framework-only
    // and has zero dependency on the domain package, same as email/hashedPassword above.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", schema = "iam", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    private Set<String> roles = new HashSet<>();

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "failed_sign_in_count", nullable = false)
    private int failedSignInCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Embedded
    private ActivationCodeEmbeddable activationCode;

    @Embedded
    private PasswordResetTokenEmbeddable passwordResetToken;

    @Column(name = "data_consent_accepted_at")
    private Instant dataConsentAcceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public UserJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFailedSignInCount() {
        return failedSignInCount;
    }

    public void setFailedSignInCount(int failedSignInCount) {
        this.failedSignInCount = failedSignInCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public ActivationCodeEmbeddable getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(ActivationCodeEmbeddable activationCode) {
        this.activationCode = activationCode;
    }

    public PasswordResetTokenEmbeddable getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(PasswordResetTokenEmbeddable passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public Instant getDataConsentAcceptedAt() {
        return dataConsentAcceptedAt;
    }

    public void setDataConsentAcceptedAt(Instant dataConsentAcceptedAt) {
        this.dataConsentAcceptedAt = dataConsentAcceptedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
