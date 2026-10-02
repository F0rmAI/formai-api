package com.formai.api.tracking.domain.model.events;

import java.time.LocalDate;
import java.util.UUID;

public record WorkoutSessionScheduled(UUID workoutSessionId, UUID clientId, LocalDate scheduledFor, String dayLabel) { }
