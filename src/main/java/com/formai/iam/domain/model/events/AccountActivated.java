package com.formai.iam.domain.model.events;

import java.time.Instant;
import java.util.UUID;

public record AccountActivated(UUID userId, Instant activatedAt) { }
