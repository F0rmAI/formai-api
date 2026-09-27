package com.formai.api.tracking.domain.model.valueobjects;

import java.util.List;

public record ProgressReport(ClientId clientId,
                             ReportPeriod period,
                             Adherence adherence,
                             int scheduled,
                             int completed,
                             int partial,
                             int skipped,
                             List<ExerciseMetric> exercises,
                             boolean hasData) { }
