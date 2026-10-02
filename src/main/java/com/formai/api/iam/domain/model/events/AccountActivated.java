package com.formai.api.iam.domain.model.events;

import java.time.Instant;
import java.util.UUID;

// email: the one the client chose on activation; the clients context copies it to its Client.
public record AccountActivated(UUID userId, String email, Instant activatedAt) { }
