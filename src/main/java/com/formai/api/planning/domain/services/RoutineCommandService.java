package com.formai.api.planning.domain.services;

import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.commands.CloseRoutineCommand;
import com.formai.api.planning.domain.model.commands.CreateRoutineCommand;
import com.formai.api.planning.domain.model.commands.DuplicateRoutineCommand;
import com.formai.api.planning.domain.model.commands.MarkRoutineActiveCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;

import java.util.Optional;

public interface RoutineCommandService {

    Optional<Routine> handle(CreateRoutineCommand command);

    Optional<Routine> handle(UpdateRoutineCommand command);

    Optional<Routine> handle(DuplicateRoutineCommand command);

    void handle(MarkRoutineActiveCommand command);

    void handle(CloseRoutineCommand command);
}
