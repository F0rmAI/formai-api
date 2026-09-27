package com.formai.api.tracking.domain.model.valueobjects;

import java.util.List;

public record ExerciseProgress(ExerciseId exerciseId,
                               ProgressWindow window,
                               List<ProgressPoint> points,
                               boolean enoughData) { }
