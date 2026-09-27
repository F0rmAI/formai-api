package com.formai.api.tracking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.UUID;

public record ExerciseMetricResource(UUID exerciseId,
                                     String exerciseName,
                                     BigDecimal firstMaxLoadKg,
                                     BigDecimal lastMaxLoadKg,
                                     BigDecimal firstVolumeKg,
                                     BigDecimal lastVolumeKg) { }
