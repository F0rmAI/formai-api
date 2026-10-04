package com.formai.api.tracking.domain.model.valueobjects;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;

import java.util.Map;
import java.util.Optional;

// lastSessionByDay: the latest session of each routine day (by order) of the current routine.
public record TodayPlan(ActiveRoutine routine, Optional<WorkoutSession> todaySession,
                        Map<Integer, WorkoutSession> lastSessionByDay) {

    public TodayPlan {
        lastSessionByDay = Map.copyOf(lastSessionByDay);
    }
}
