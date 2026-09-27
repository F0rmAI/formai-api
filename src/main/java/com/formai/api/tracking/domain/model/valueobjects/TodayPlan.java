package com.formai.api.tracking.domain.model.valueobjects;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;

import java.util.Optional;

public record TodayPlan(ActiveRoutine routine, Optional<WorkoutSession> todaySession) { }
