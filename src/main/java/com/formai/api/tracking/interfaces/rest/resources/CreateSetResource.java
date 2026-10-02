package com.formai.api.tracking.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSetResource(@NotNull UUID exerciseId,
                                @NotNull Integer setNumber,
                                @NotNull BigDecimal loadKg,
                                @NotNull Integer reps) { }
