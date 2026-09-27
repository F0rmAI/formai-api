package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.RoutineAssigned;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

// Explicit bean name: planning has an event handler with the same simple name.
// Two listeners, one aggregate each: the first syncs the ActiveRoutine and commits, then the
// second schedules today's session from it, so a routine starting today can be trained today
// instead of waiting for WorkoutSessionDailyJob.
@Component("trackingRoutineAssignedEventHandler")
public class RoutineAssignedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(RoutineAssignedEventHandler.class);

    private final ActiveRoutineCommandService activeRoutineCommandService;
    private final WorkoutSessionCommandService workoutSessionCommandService;

    public RoutineAssignedEventHandler(ActiveRoutineCommandService activeRoutineCommandService,
                                       WorkoutSessionCommandService workoutSessionCommandService) {
        this.activeRoutineCommandService = activeRoutineCommandService;
        this.workoutSessionCommandService = workoutSessionCommandService;
    }

    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(RoutineAssigned event) {
        activeRoutineCommandService.handle(new SyncActiveRoutineCommand(new ClientId(event.clientId())));
    }

    @Order(2)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleToday(RoutineAssigned event) {
        scheduleOn(event, LocalDate.now());
    }

    // Idempotent: scheduling returns the session already scheduled for that date, if any.
    public void scheduleOn(RoutineAssigned event, LocalDate today) {
        if (event.startDate().isAfter(today)) {
            return;
        }
        var clientId = new ClientId(event.clientId());
        try {
            workoutSessionCommandService.handle(new ScheduleWorkoutSessionCommand(clientId, today));
        } catch (RuntimeException ex) {
            log.error("Could not schedule today's workout session for client {}", clientId.value(), ex);
        }
    }
}
