package com.formai.api.tracking.domain.model.queries;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;

public record GetWorkoutSessionByIdQuery(WorkoutSessionId workoutSessionId,
                                         ClientId clientId,
                                         String requesterHolderId) { }
