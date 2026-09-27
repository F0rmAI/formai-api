package com.formai.api.tracking.domain.model.events;

import java.util.UUID;

public record WorkoutSessionFinished(UUID workoutSessionId, UUID clientId, String status) { }
