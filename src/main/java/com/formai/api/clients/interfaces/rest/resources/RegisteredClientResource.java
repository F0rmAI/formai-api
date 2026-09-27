package com.formai.api.clients.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

// The activation code is shown on screen once, for the trainer to share by hand (FR-003).
public record RegisteredClientResource(UUID id,
                                       String fullName,
                                       String email,
                                       String status,
                                       String activationCode,
                                       Instant activationCodeExpiresAt) { }
