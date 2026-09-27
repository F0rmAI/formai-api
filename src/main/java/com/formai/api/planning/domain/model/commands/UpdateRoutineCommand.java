package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;

import java.util.List;

public record UpdateRoutineCommand(RoutineId routineId,
                                   String holderId,
                                   RoutineName name,
                                   List<RoutineSession> sessions) { }
