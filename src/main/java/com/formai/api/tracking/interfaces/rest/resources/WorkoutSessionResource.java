package com.formai.api.tracking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// finishedAt is null until the session is finished.
public record WorkoutSessionResource(UUID id,
                                     LocalDate scheduledFor,
                                     String dayLabel,
                                     int routineVersion,
                                     String status,
                                     BigDecimal totalVolumeKg,
                                     Instant finishedAt,
                                     List<SessionExerciseResource> exercises) { }
