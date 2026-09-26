package com.formai.iam.domain.model.entities;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public class PasswordResetToken {

    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;

    // Never persisted: only the hash is stored. Present only on the instance that issued
    // the token, and has no setter so MapStruct never treats it as a mapping target.
    private String rawValue;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public PasswordResetToken() {
    }

    public PasswordResetToken(String rawValue, Instant expiresAt) {
        this.rawValue = rawValue;
        this.tokenHash = hashOf(rawValue);
        this.expiresAt = expiresAt;
    }

    public static String hashOf(String rawValue) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
