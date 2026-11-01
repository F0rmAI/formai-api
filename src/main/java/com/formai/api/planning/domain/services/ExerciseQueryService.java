package com.formai.api.planning.domain.services;

import com.formai.api.planning.domain.model.queries.GetExerciseByIdQuery;
import com.formai.api.planning.domain.model.queries.GetExercisesQuery;
import com.formai.api.planning.domain.model.queries.GetMuscleGroupsQuery;
import com.formai.api.planning.domain.model.valueobjects.ExerciseSummary;
import com.formai.api.planning.domain.model.valueobjects.ExerciseSummaryPage;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;

import java.util.List;
import java.util.Optional;

public interface ExerciseQueryService {

    ExerciseSummaryPage handle(GetExercisesQuery query);

    Optional<ExerciseSummary> handle(GetExerciseByIdQuery query);

    List<MuscleGroup> handle(GetMuscleGroupsQuery query);
}
