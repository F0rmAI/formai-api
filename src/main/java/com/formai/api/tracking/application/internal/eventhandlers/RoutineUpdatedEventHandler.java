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

// Resilience: self-healing. If this fails, WorkoutSessionDailyJob syncs every active routine with
// planning before scheduling the day's session, so the new version arrives the next day. Sessions
// already recorded keep their routineVersion (FR-011).
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
