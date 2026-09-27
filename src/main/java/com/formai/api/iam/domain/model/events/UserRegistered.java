package com.formai.api.iam.domain.model.events;

import java.util.UUID;

public record UserRegistered(UUID userId, String holderId) { }
