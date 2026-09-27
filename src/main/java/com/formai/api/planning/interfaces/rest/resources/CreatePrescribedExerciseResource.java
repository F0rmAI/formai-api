package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

// Values of zero or less are a business rule violation (422, FR-008), so they are
// validated by the domain rather than rejected here with a 400.
public record CreatePrescribedExerciseResource(@NotNull UUID exerciseId,
                                               @NotNull Integer sets,
                                               @NotNull Integer reps,
                                               @NotNull BigDecimal targetLoadKg,
                                               @NotNull Integer restSeconds) { }
