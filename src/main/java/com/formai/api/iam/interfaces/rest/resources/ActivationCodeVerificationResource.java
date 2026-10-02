package com.formai.api.iam.interfaces.rest.resources;

import java.time.Instant;

public record ActivationCodeVerificationResource(Instant expiresAt) { }
