package com.formai.api.clients.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record ClientResource(UUID id, String fullName, String email, String status, Instant registeredAt) { }
