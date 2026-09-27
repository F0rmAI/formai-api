package com.formai.api.tracking.domain.services;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;

import java.util.Optional;

public interface ActiveRoutineCommandService {

    Optional<ActiveRoutine> handle(SyncActiveRoutineCommand command);

    void handle(EndActiveRoutineCommand command);
}
