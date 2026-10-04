package com.formai.api.tracking.application.internal.queryservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalPlanningService;
import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutineQuery;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesAtQuery;
import com.formai.api.tracking.domain.model.queries.GetClientTodayQuery;
import com.formai.api.tracking.domain.model.valueobjects.TodayPlan;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Override
    public Optional<TodayPlan> handle(GetActiveRoutineQuery query) {
        var routine = activeRoutineRepository.findByClientId(query.clientId())
                .filter(candidate -> candidate.isActiveOn(query.today()))
                .or(() -> externalPlanningService.fetchActiveRoutine(query.clientId())
                        .map(plan -> ActiveRoutine.syncFrom(new SyncActiveRoutineCommand(query.clientId()), plan))
                        .filter(candidate -> candidate.isActiveOn(query.today())))
                .orElseThrow(ActiveRoutineNotFoundException::new);
        var todaySession = workoutSessionRepository.findByClientIdAndScheduledFor(query.clientId(), query.today());
        return Optional.of(new TodayPlan(routine, todaySession, lastSessionByDay(routine)));
    }

    // Every time zone is between UTC-18:00 and UTC+18:00, so the routines active somewhere at that
    // instant are active between those two dates; each one is then checked on its client's date.
    @Override
    public List<ActiveRoutine> handle(GetActiveRoutinesAtQuery query) {
        var earliest = LocalDate.ofInstant(query.now(), ZoneOffset.MIN);
        var latest = LocalDate.ofInstant(query.now(), ZoneOffset.MAX);
        return activeRoutineRepository.findAllActiveBetween(earliest, latest).stream()
                .filter(routine -> routine.isActiveOn(routine.today(query.now())))
                .toList();
    }

    @Override
    public LocalDate handle(GetClientTodayQuery query) {
        return activeRoutineRepository.findByClientId(query.clientId())
                .map(routine -> routine.today(query.now()))
                .orElseGet(() -> LocalDate.ofInstant(query.now(), ActiveRoutine.DEFAULT_TIME_ZONE));
    }

    // The latest session of each routine day, so the client sees how each day went last time.
    private Map<Integer, WorkoutSession> lastSessionByDay(ActiveRoutine routine) {
        return workoutSessionRepository.findAllByClientIdAndRoutineId(routine.getClientId(), routine.getRoutineId())
                .stream()
                .collect(Collectors.toMap(WorkoutSession::getDayOrder, Function.identity(), (latest, older) -> latest));
    }
}
