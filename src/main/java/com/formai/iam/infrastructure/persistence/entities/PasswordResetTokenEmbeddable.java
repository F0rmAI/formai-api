package com.formai.iam.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public class PasswordResetTokenEmbeddable {

    @Column(name = "password_reset_token_hash", unique = true, length = 64)
    private String tokenHash;

    @Column(name = "password_reset_token_expires_at")
    private Instant expiresAt;

    @Column(name = "password_reset_token_used_at")
    private Instant usedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public PasswordResetTokenEmbeddable() {
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
