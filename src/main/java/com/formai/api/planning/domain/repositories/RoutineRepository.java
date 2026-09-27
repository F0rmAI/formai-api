package com.formai.api.planning.domain.repositories;

import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.Pagination;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutinePage;

import java.util.Optional;

public interface RoutineRepository {

    Routine save(Routine routine);

    Optional<Routine> findByIdAndHolderId(RoutineId id, String holderId);

    Optional<Routine> findById(RoutineId id);

    RoutinePage findAllByHolderId(String holderId, Pagination pagination);

    boolean existsByHolderIdAndExerciseId(String holderId, ExerciseId exerciseId);
}
