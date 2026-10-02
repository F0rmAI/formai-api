package com.formai.api.planning.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.commands.MarkRoutineActiveCommand;
import com.formai.api.planning.domain.model.events.RoutineAssigned;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.services.RoutineCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Explicit bean name: tracking has an event handler with the same simple name.
// Resilience: self-healing. One transaction, one aggregate: assigning changes the ClientPlan and
// this marks the Routine ACTIVE after commit. If it fails only the status the trainer sees is
// stale, and the next assignment of that routine runs it again (MarkRoutineActive is idempotent).
@Component("planningRoutineAssignedEventHandler")
public class RoutineAssignedEventHandler {

    private final RoutineCommandService routineCommandService;

    public RoutineAssignedEventHandler(RoutineCommandService routineCommandService) {
        this.routineCommandService = routineCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(RoutineAssigned event) {
        routineCommandService.handle(new MarkRoutineActiveCommand(new RoutineId(event.routineId())));
    }
}
