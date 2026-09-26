package com.formai.iam.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public class ActivationCodeEmbeddable {

    @Column(name = "activation_code", unique = true, length = 16)
    private String code;

    @Column(name = "activation_code_expires_at")
    private Instant expiresAt;

    @Column(name = "activation_code_used_at")
    private Instant usedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ActivationCodeEmbeddable() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
