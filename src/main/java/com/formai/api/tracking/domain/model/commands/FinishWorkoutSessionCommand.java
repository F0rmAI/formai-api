package com.formai.api.tracking.domain.model.commands;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;

public record FinishWorkoutSessionCommand(WorkoutSessionId workoutSessionId,
                                          ClientId clientId,
                                          boolean confirmPartial) { }
