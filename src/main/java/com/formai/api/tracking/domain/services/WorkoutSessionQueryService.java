package com.formai.api.tracking.domain.services;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.queries.GetClientOverviewsQuery;
import com.formai.api.tracking.domain.model.queries.GetExerciseProgressQuery;
import com.formai.api.tracking.domain.model.queries.GetProgressReportQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutHistoryQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutSessionByIdQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientOverviewPage;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseProgress;
import com.formai.api.tracking.domain.model.valueobjects.ProgressReport;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;

import java.util.Optional;

public interface WorkoutSessionQueryService {

    Optional<WorkoutSession> handle(GetWorkoutSessionByIdQuery query);

    WorkoutSessionPage handle(GetWorkoutHistoryQuery query);

    ProgressReport handle(GetProgressReportQuery query);

    ExerciseProgress handle(GetExerciseProgressQuery query);

    ClientOverviewPage handle(GetClientOverviewsQuery query);
}
