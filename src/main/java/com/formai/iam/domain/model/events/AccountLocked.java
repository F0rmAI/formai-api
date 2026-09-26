package com.formai.iam.domain.model.events;

import java.time.Instant;
import java.util.UUID;

public record AccountLocked(UUID userId, Instant lockedUntil) { }
