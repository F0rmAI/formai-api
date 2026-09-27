package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.RoutineAssigned;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Explicit bean name: planning has an event handler with the same simple name.
@Component("trackingRoutineAssignedEventHandler")
public class RoutineAssignedEventHandler {

    private final ActiveRoutineCommandService activeRoutineCommandService;

    public RoutineAssignedEventHandler(ActiveRoutineCommandService activeRoutineCommandService) {
        this.activeRoutineCommandService = activeRoutineCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(RoutineAssigned event) {
        activeRoutineCommandService.handle(new SyncActiveRoutineCommand(new ClientId(event.clientId())));
    }
}
