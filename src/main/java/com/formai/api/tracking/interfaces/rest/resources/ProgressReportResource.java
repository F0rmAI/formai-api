package com.formai.api.tracking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProgressReportResource(UUID clientId,
                                     LocalDate from,
                                     LocalDate to,
                                     BigDecimal adherencePercentage,
                                     int scheduled,
                                     int completed,
                                     int partial,
                                     int skipped,
                                     List<ExerciseMetricResource> exercises,
                                     boolean hasData) { }
