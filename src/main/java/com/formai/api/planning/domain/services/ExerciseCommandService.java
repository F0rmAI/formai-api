package com.formai.api.planning.domain.services;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.commands.ArchiveExerciseCommand;
import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.commands.DeleteExerciseCommand;
import com.formai.api.planning.domain.model.commands.RestoreExerciseCommand;

import java.util.Optional;

public interface ExerciseCommandService {

    Optional<Exercise> handle(CreateExerciseCommand command);

    Optional<Exercise> handle(ArchiveExerciseCommand command);

    Optional<Exercise> handle(RestoreExerciseCommand command);

    void handle(DeleteExerciseCommand command);
}
