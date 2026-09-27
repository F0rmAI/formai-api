package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;

import java.time.LocalDate;

public record AssignRoutineCommand(RoutineId routineId, ClientId clientId, String holderId, LocalDate startDate) { }
