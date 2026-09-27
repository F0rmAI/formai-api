package com.formai.api.clients.domain.model.valueobjects;

import java.time.Instant;

// The code the trainer shows on screen so the client can activate their account.
public record ActivationTicket(ClientId clientId, String code, Instant expiresAt) { }
