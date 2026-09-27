package com.formai.api.planning.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.UUID;

public record PrescribedExerciseResource(UUID exerciseId,
                                         String exerciseName,
                                         int sets,
                                         int reps,
                                         BigDecimal targetLoadKg,
                                         int restSeconds) { }
