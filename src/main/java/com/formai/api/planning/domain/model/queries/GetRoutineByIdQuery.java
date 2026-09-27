package com.formai.api.planning.domain.model.queries;

import com.formai.api.planning.domain.model.valueobjects.RoutineId;

public record GetRoutineByIdQuery(RoutineId routineId, String holderId) { }
