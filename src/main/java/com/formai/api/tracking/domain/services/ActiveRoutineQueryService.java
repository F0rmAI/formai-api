package com.formai.api.tracking.domain.services;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutineQuery;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesOnQuery;
import com.formai.api.tracking.domain.model.valueobjects.TodayPlan;

import java.util.List;
import java.util.Optional;

public interface ActiveRoutineQueryService {

    Optional<TodayPlan> handle(GetActiveRoutineQuery query);

    List<ActiveRoutine> handle(GetActiveRoutinesOnQuery query);
}
