package com.formai.iam.domain.model.entities;

import java.time.Instant;

public class ActivationCode {

    private String code;
    private Instant expiresAt;
    private Instant usedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ActivationCode() {
    }

    public ActivationCode(String code, Instant expiresAt) {
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public String getCode() {
        return code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
