package com.formai.api.tracking.domain.model.valueobjects;

public record ExerciseMetric(ExerciseId exerciseId,
                             String exerciseName,
                             Load firstMaxLoad,
                             Load lastMaxLoad,
                             TrainingVolume firstVolume,
                             TrainingVolume lastVolume) { }
