package com.formai.api.iam.domain.model.aggregates;

import com.formai.api.iam.domain.model.commands.IssueRefreshTokenCommand;
import com.formai.api.iam.domain.model.entities.PasswordResetToken;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

// Long-lived token (NFR-007) that renews the short-lived access JWT without asking for the
// password again. It rotates on every use and is revoked on sign-out or when the account is
// disabled. Like PasswordResetToken, only its SHA-256 hash is stored.
public class RefreshToken {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant revokedAt;
    private Instant createdAt;

    // Never persisted: only the hash is stored. Present only on the instance that issued
    // the token, and has no setter so MapStruct never treats it as a mapping target.
    private String rawValue;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public RefreshToken() {
    }

    public static RefreshToken issue(IssueRefreshTokenCommand command, Instant now, Duration validity) {
        var token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.userId = command.userId();
        token.rawValue = randomUrlSafeToken();
        token.tokenHash = hashOf(token.rawValue);
        token.expiresAt = now.plus(validity);
        token.createdAt = now;
        return token;
    }

    public static String hashOf(String rawValue) {
        return PasswordResetToken.hashOf(rawValue);
    }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    private static String randomUrlSafeToken() {
        var bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<String> getRawValue() {
        return Optional.ofNullable(rawValue);
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
