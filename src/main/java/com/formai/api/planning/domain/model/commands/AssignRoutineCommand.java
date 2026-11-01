package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.AssignmentNote;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;

import java.time.LocalDate;
import java.util.Optional;

public record AssignRoutineCommand(RoutineId routineId, ClientId clientId, String holderId, LocalDate startDate,
                                   TrainingDays trainingDays, Optional<AssignmentNote> note) {

    public AssignRoutineCommand(RoutineId routineId, ClientId clientId, String holderId, LocalDate startDate,
                                TrainingDays trainingDays) {
        this(routineId, clientId, holderId, startDate, trainingDays, Optional.empty());
    }
}
