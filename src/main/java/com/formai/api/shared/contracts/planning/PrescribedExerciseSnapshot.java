package com.formai.api.shared.contracts.planning;

import java.math.BigDecimal;
import java.util.UUID;

public record PrescribedExerciseSnapshot(UUID exerciseId,
                                         String exerciseName,
                                         int sets,
                                         int reps,
                                         BigDecimal targetLoadKg,
                                         int restSeconds) { }
