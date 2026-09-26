package com.formai.iam.domain.model.events;

import java.time.Instant;
import java.util.UUID;

public record ActivationCodeIssued(UUID userId, Instant expiresAt) { }
