package com.formai.api.tracking.domain.model.commands;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;

public record RecordSetCommand(WorkoutSessionId workoutSessionId,
                               ClientId clientId,
                               ExerciseId exerciseId,
                               int setNumber,
                               Load load,
                               Reps reps) { }
