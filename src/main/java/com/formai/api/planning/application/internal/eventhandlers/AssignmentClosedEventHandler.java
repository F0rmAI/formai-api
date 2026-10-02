package com.formai.api.planning.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.commands.CloseRoutineCommand;
import com.formai.api.planning.domain.model.events.AssignmentClosed;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.services.RoutineCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Explicit bean name: tracking has an event handler with the same simple name.
// Resilience: self-healing. If this fails the Routine stays ACTIVE with no clients, which only
// changes the status the trainer sees (tracking follows the assignments, not this status); the
// next assignment or closing of that routine evaluates it again.
@Component("planningAssignmentClosedEventHandler")
public class AssignmentClosedEventHandler {

    private final RoutineCommandService routineCommandService;

    public AssignmentClosedEventHandler(RoutineCommandService routineCommandService) {
        this.routineCommandService = routineCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AssignmentClosed event) {
        routineCommandService.handle(new CloseRoutineCommand(new RoutineId(event.routineId())));
    }
}
