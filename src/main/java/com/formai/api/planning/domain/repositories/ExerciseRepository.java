package com.formai.api.planning.domain.repositories;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.ExerciseOrigin;
import com.formai.api.planning.domain.model.valueobjects.ExercisePage;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.domain.model.valueobjects.Pagination;

import java.util.Optional;

public interface ExerciseRepository {

    Exercise save(Exercise exercise);

    Optional<Exercise> findByIdAndHolderId(ExerciseId id, String holderId);

    // An exercise of the global catalog or of the trainer's own ones.
    Optional<Exercise> findAvailableById(ExerciseId id, String holderId);

    // Whether the name (ignoring case) is taken in the global catalog or among the trainer's own exercises.
    boolean existsAvailableByName(String holderId, ExerciseName name);

    ExercisePage findAllAvailableTo(String holderId, Optional<String> search, Optional<MuscleGroup> muscleGroup,
                                    Optional<ExerciseOrigin> origin, Optional<ExerciseStatus> status,
                                    Pagination pagination);

    void delete(Exercise exercise);
}
