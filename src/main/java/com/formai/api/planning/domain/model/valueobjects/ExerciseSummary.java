package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.model.aggregates.Exercise;

// An exercise as the trainer sees it in the catalog: with the number of their routines that prescribe it.
public record ExerciseSummary(Exercise exercise, long routineCount) { }
