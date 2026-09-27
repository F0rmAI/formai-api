package com.formai.api.tracking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SessionExerciseResource(UUID exerciseId,
                                      String exerciseName,
                                      int targetSets,
                                      int targetReps,
                                      BigDecimal targetLoadKg,
                                      List<SetEntryResource> sets) { }
