package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePrescribedExerciseResource(@NotNull UUID exerciseId,
                                               @NotNull Integer sets,
                                               @NotNull Integer reps,
                                               @NotNull BigDecimal targetLoadKg,
                                               @NotNull Integer restSeconds) { }
