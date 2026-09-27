package com.formai.api.planning.domain.services;

import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.entities.RoutineVersion;
import com.formai.api.planning.domain.model.queries.GetRoutineByIdQuery;
import com.formai.api.planning.domain.model.queries.GetRoutineVersionsQuery;
import com.formai.api.planning.domain.model.queries.GetRoutinesQuery;
import com.formai.api.planning.domain.model.valueobjects.RoutinePage;

import java.util.List;
import java.util.Optional;

public interface RoutineQueryService {

    RoutinePage handle(GetRoutinesQuery query);

    Optional<Routine> handle(GetRoutineByIdQuery query);

    List<RoutineVersion> handle(GetRoutineVersionsQuery query);
}
