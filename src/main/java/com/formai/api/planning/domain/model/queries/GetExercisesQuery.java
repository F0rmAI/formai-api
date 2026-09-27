package com.formai.api.planning.domain.model.queries;

import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.Pagination;

import java.util.Optional;

public record GetExercisesQuery(String holderId,
                                Optional<String> search,
                                Optional<ExerciseStatus> status,
                                Pagination pagination) { }
