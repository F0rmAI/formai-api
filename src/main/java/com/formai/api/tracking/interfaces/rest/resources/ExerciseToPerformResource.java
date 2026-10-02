package com.formai.api.tracking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.UUID;

public record ExerciseToPerformResource(UUID exerciseId,
                                        String exerciseName,
                                        int sets,
                                        int reps,
                                        BigDecimal targetLoadKg,
                                        int restSeconds) { }
