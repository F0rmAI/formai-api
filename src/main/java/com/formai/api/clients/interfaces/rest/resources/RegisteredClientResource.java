package com.formai.api.clients.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record RegisteredClientResource(UUID id,
                                       String fullName,
                                       String status,
                                       String activationCode,
                                       Instant activationCodeExpiresAt) { }
