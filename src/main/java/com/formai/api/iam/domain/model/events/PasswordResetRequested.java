package com.formai.api.iam.domain.model.events;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequested(UUID userId, String holderId, String email, String token, Instant expiresAt) { }
