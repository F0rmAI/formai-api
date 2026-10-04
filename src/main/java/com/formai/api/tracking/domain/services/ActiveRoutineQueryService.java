package com.formai.api.tracking.domain.services;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutineQuery;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesAtQuery;
import com.formai.api.tracking.domain.model.queries.GetClientTodayQuery;
import com.formai.api.tracking.domain.model.valueobjects.TodayPlan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActiveRoutineQueryService {

    Optional<TodayPlan> handle(GetActiveRoutineQuery query);

    List<ActiveRoutine> handle(GetActiveRoutinesAtQuery query);

    LocalDate handle(GetClientTodayQuery query);
}
