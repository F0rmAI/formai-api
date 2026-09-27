package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;

public record DuplicateRoutineCommand(RoutineId routineId, String holderId, RoutineName name) { }
