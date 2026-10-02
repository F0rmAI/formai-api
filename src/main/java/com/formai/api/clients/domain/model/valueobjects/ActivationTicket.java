package com.formai.api.clients.domain.model.valueobjects;

import java.time.Instant;

public record ActivationTicket(ClientId clientId, String code, Instant expiresAt) { }
