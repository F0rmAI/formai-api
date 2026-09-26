package com.formai.shared.contracts.iam;

import java.time.Instant;
import java.util.UUID;

public record AccountActivationSummary(UUID userId, String activationCode, Instant expiresAt) { }
