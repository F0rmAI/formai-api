package com.formai.api.clients.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record ActivationCodeResource(UUID clientId, String activationCode, Instant expiresAt) { }
