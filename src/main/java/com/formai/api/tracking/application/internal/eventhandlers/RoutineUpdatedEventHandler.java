package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.RoutineUpdated;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutinesOfRoutineCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesAtQuery;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.springframework.core.annotation.Order;

import java.time.Instant;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Resilience: self-healing. If this fails, WorkoutSessionDailyJob syncs every active routine with
// planning before scheduling the day's session, so the new version arrives the next day. Sessions
// already recorded keep their routineVersion (FR-011).
@Component
public class RoutineUpdatedEventHandler {

    private final ActiveRoutineCommandService activeRoutineCommandService;

    private final ActiveRoutineQueryService activeRoutineQueryService;
    private final WorkoutSessionCommandService workoutSessionCommandService;

    public RoutineUpdatedEventHandler(ActiveRoutineCommandService activeRoutineCommandService,
                                     ActiveRoutineQueryService activeRoutineQueryService,
                                     WorkoutSessionCommandService workoutSessionCommandService) {
        this.activeRoutineCommandService = activeRoutineCommandService;
        this.activeRoutineQueryService = activeRoutineQueryService;
        this.workoutSessionCommandService = workoutSessionCommandService;
    }

    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(RoutineUpdated event) {
        activeRoutineCommandService.handle(new SyncActiveRoutinesOfRoutineCommand(new RoutineId(event.routineId())));
    }
    // Run after synchronization has committed so scheduling reads the latest prescriptions.
    @Order(2)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleToday(RoutineUpdated event) {
        var now = Instant.now();
        activeRoutineQueryService.handle(new GetActiveRoutinesAtQuery(now)).stream()
                .filter(routine -> routine.getRoutineId().value().equals(event.routineId()))
                .forEach(routine -> workoutSessionCommandService.handle(
                        new ScheduleWorkoutSessionCommand(routine.getClientId(), routine.today(now))));
    }
}
