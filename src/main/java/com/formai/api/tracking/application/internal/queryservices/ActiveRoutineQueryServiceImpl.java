package com.formai.api.tracking.application.internal.queryservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalPlanningService;
import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutineQuery;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesOnQuery;
import com.formai.api.tracking.domain.model.valueobjects.TodayPlan;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ActiveRoutineQueryServiceImpl implements ActiveRoutineQueryService {

    private final ActiveRoutineRepository activeRoutineRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final ExternalPlanningService externalPlanningService;

    public ActiveRoutineQueryServiceImpl(ActiveRoutineRepository activeRoutineRepository,
                                         WorkoutSessionRepository workoutSessionRepository,
                                         ExternalPlanningService externalPlanningService) {
        this.activeRoutineRepository = activeRoutineRepository;
        this.workoutSessionRepository = workoutSessionRepository;
        this.externalPlanningService = externalPlanningService;
    }

    // A safe read: it never creates anything. When the local copy is missing (the sync
    // event failed) it shows planning's routine without storing it. Either way a routine
    // only shows from its start date (FR-010).
    @Override
    public Optional<TodayPlan> handle(GetActiveRoutineQuery query) {
        var routine = activeRoutineRepository.findByClientId(query.clientId())
                .filter(candidate -> candidate.isActiveOn(query.today()))
                .or(() -> externalPlanningService.fetchActiveRoutine(query.clientId())
                        .map(plan -> ActiveRoutine.syncFrom(new SyncActiveRoutineCommand(query.clientId()), plan))
                        .filter(candidate -> candidate.isActiveOn(query.today())))
                .orElseThrow(ActiveRoutineNotFoundException::new);
        var todaySession = workoutSessionRepository.findByClientIdAndScheduledFor(query.clientId(), query.today());
        return Optional.of(new TodayPlan(routine, todaySession));
    }

    @Override
    public List<ActiveRoutine> handle(GetActiveRoutinesOnQuery query) {
        return activeRoutineRepository.findAllActiveOn(query.date());
    }
}
