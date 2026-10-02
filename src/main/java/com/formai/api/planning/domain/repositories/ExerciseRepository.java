package com.formai.api.planning.domain.repositories;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.ExercisePage;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.Pagination;

import java.util.Optional;

public interface ExerciseRepository {

    Exercise save(Exercise exercise);

    Optional<Exercise> findByIdAndHolderId(ExerciseId id, String holderId);

    boolean existsByHolderIdAndName(String holderId, ExerciseName name);

    ExercisePage findAllByHolderId(String holderId, Optional<String> search, Optional<ExerciseStatus> status,
                                   Pagination pagination);

    void delete(Exercise exercise);
}
