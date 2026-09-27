package com.formai.api.tracking.domain.model.valueobjects;

import java.time.LocalDate;

// The date of a client's most recent session that was actually trained (completed or partial).
public record LastWorkout(ClientId clientId, LocalDate lastWorkoutOn) { }
