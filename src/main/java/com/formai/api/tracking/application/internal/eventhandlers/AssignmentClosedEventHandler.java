package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.AssignmentClosed;
import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

// Resilience: self-healing. If this fails, WorkoutSessionDailyJob finds no current assignment in
// planning for that client and ends the ActiveRoutine before scheduling new sessions.
// Two listeners, one aggregate each: the first ends the ActiveRoutine, the second lets today's
// session follow it, so an untouched session of the routine that just ended is not left behind.
@Component
public class AssignmentClosedEventHandler {

    private final ActiveRoutineCommandService activeRoutineCommandService;
    private final WorkoutSessionCommandService workoutSessionCommandService;

    public AssignmentClosedEventHandler(ActiveRoutineCommandService activeRoutineCommandService,
                                        WorkoutSessionCommandService workoutSessionCommandService) {
        this.activeRoutineCommandService = activeRoutineCommandService;
        this.workoutSessionCommandService = workoutSessionCommandService;
    }

    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AssignmentClosed event) {
        activeRoutineCommandService.handle(new EndActiveRoutineCommand(new ClientId(event.clientId()), event.endDate()));
    }

    @Order(2)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rescheduleToday(AssignmentClosed event) {
        rescheduleOn(event, LocalDate.now());
    }

    public void rescheduleOn(AssignmentClosed event, LocalDate today) {
        try {
            workoutSessionCommandService.handle(new ScheduleWorkoutSessionCommand(new ClientId(event.clientId()), today));
        } catch (ActiveRoutineNotFoundException ex) {
            // Expected when no routine replaces the one that ended: there is nothing to schedule.
        }
    }
}
