package com.formai.api.tracking.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

// Negative load or reps are a business rule violation (422), not a malformed request,
// so they are validated by the domain rather than rejected here with a 400.
public record CreateSetCorrectionResource(@NotNull UUID exerciseId,
                                          @NotNull Integer setNumber,
                                          @NotNull BigDecimal loadKg,
                                          @NotNull Integer reps) { }
