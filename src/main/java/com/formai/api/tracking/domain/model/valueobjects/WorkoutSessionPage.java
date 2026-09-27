package com.formai.api.tracking.domain.model.valueobjects;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;

import java.util.List;

public record WorkoutSessionPage(List<WorkoutSession> items,
                                 int page,
                                 int size,
                                 long totalElements,
                                 int totalPages) { }
