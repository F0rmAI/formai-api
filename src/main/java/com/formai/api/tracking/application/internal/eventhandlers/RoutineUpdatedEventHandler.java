package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.RoutineUpdated;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutinesOfRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RoutineUpdatedEventHandler {

    private final ActiveRoutineCommandService activeRoutineCommandService;

    public RoutineUpdatedEventHandler(ActiveRoutineCommandService activeRoutineCommandService) {
        this.activeRoutineCommandService = activeRoutineCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(RoutineUpdated event) {
        activeRoutineCommandService.handle(new SyncActiveRoutinesOfRoutineCommand(new RoutineId(event.routineId())));
    }
}
