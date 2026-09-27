package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;

import java.util.List;

public record CreateRoutineCommand(String holderId, RoutineName name, List<RoutineSession> sessions) { }
