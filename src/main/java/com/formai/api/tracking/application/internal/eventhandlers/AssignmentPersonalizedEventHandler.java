package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.AssignmentPersonalized;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.queries.GetClientTodayQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

// Separate ordered transactions: synchronize ActiveRoutine, then refresh today's untouched workout.
// Resilience: self-healing. The daily job retries synchronization and scheduling. A workout with
// recorded sets keeps its original prescription and version, preserving the client's history.
@Component("trackingAssignmentPersonalizedEventHandler")
public class AssignmentPersonalizedEventHandler {

    private final ActiveRoutineCommandService activeRoutineCommandService;
    private final ActiveRoutineQueryService activeRoutineQueryService;
    private final WorkoutSessionCommandService workoutSessionCommandService;

    public AssignmentPersonalizedEventHandler(ActiveRoutineCommandService activeRoutineCommandService,
                                              ActiveRoutineQueryService activeRoutineQueryService,
                                              WorkoutSessionCommandService workoutSessionCommandService) {
        this.activeRoutineCommandService = activeRoutineCommandService;
        this.activeRoutineQueryService = activeRoutineQueryService;
        this.workoutSessionCommandService = workoutSessionCommandService;
    }

    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AssignmentPersonalized event) {
        activeRoutineCommandService.handle(new SyncActiveRoutineCommand(new ClientId(event.clientId())));
    }

    @Order(2)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleToday(AssignmentPersonalized event) {
        var clientId = new ClientId(event.clientId());
        var today = activeRoutineQueryService.handle(new GetClientTodayQuery(clientId, Instant.now()));
        workoutSessionCommandService.handle(new ScheduleWorkoutSessionCommand(clientId, today));
    }
}
