package com.formai.api.planning.domain.services;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.queries.GetExerciseByIdQuery;
import com.formai.api.planning.domain.model.queries.GetExercisesQuery;
import com.formai.api.planning.domain.model.valueobjects.ExercisePage;

import java.util.Optional;

public interface ExerciseQueryService {

    ExercisePage handle(GetExercisesQuery query);

    Optional<Exercise> handle(GetExerciseByIdQuery query);
}
