package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.AssignmentClosed;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Resilience: self-healing. If this fails, WorkoutSessionDailyJob finds no current assignment in
// planning for that client and ends the ActiveRoutine before scheduling new sessions.
@Component
public class AssignmentClosedEventHandler {

    private final ActiveRoutineCommandService activeRoutineCommandService;

    public AssignmentClosedEventHandler(ActiveRoutineCommandService activeRoutineCommandService) {
        this.activeRoutineCommandService = activeRoutineCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AssignmentClosed event) {
        activeRoutineCommandService.handle(new EndActiveRoutineCommand(new ClientId(event.clientId()), event.endDate()));
    }
}
