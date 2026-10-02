package com.formai.api.tracking.domain.model.commands;

import com.formai.api.tracking.domain.model.valueobjects.RoutineId;

public record SyncActiveRoutinesOfRoutineCommand(RoutineId routineId) { }
