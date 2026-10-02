package com.formai.api.planning.domain.model.queries;

import com.formai.api.planning.domain.model.valueobjects.ExerciseId;

public record GetExerciseByIdQuery(ExerciseId exerciseId, String holderId) { }
