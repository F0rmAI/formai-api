package com.formai.api.tracking.domain.model.events;

import java.util.UUID;

public record SetRecorded(UUID workoutSessionId, UUID exerciseId, int setNumber) { }
