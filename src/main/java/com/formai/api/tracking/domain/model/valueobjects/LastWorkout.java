package com.formai.api.tracking.domain.model.valueobjects;

import java.time.LocalDate;

public record LastWorkout(ClientId clientId, LocalDate lastWorkoutOn) { }
