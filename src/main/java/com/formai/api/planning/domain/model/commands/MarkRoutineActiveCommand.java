package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.RoutineId;

public record MarkRoutineActiveCommand(RoutineId routineId) { }
