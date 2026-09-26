package com.formai.iam.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record AccountActivationResource(UUID userId, String status, Instant activatedAt) { }
