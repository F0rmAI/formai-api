package com.formai.api.tracking.domain.repositories;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.LastWorkout;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkoutSessionRepository {

    WorkoutSession save(WorkoutSession session);

    Optional<WorkoutSession> findByIdAndClientId(WorkoutSessionId id, ClientId clientId);

    Optional<WorkoutSession> findByClientIdAndScheduledFor(ClientId clientId, LocalDate date);

    // Most recent first (scheduledFor DESC).
    WorkoutSessionPage findAllByClientId(ClientId clientId, Optional<ReportPeriod> period, Pagination pagination);

    Optional<WorkoutSession> findLastFinishedByClientId(ClientId clientId);

    List<WorkoutSession> findAllPendingBefore(LocalDate date);

    // Only for the clients that have trained at least once.
    List<LastWorkout> findLastWorkoutDates(List<ClientId> clientIds);
}
