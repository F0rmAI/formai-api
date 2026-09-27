package com.formai.api.tracking.domain.model.queries;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ProgressWindow;

public record GetExerciseProgressQuery(ClientId clientId,
                                       String requesterHolderId,
                                       ExerciseId exerciseId,
                                       ProgressWindow window) { }
