package com.formai.api.tracking.domain.model.events;

import java.util.UUID;

public record WorkoutSessionSkipped(UUID workoutSessionId, UUID clientId) { }
