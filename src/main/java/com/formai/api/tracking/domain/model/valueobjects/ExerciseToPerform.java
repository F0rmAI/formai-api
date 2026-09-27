package com.formai.api.tracking.domain.model.valueobjects;

import java.math.BigDecimal;

public record ExerciseToPerform(ExerciseId exerciseId,
                                String exerciseName,
                                int sets,
                                int reps,
                                BigDecimal targetLoadKg,
                                int restSeconds) { }
