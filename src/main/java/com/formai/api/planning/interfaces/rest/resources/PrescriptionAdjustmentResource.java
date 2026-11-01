package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

// The client's own target load and/or rest for an exercise of a routine session; null keeps the routine's.
public record PrescriptionAdjustmentResource(@Min(1) int sessionOrder,
                                             @NotNull UUID exerciseId,
                                             BigDecimal targetLoadKg,
                                             Integer restSeconds) { }
