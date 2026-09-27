package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.RoutineUpdated;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// A new routine version re-syncs every client following that routine. Sessions already
// scheduled keep the version they were scheduled with (FR-011).
@Component
public class RoutineUpdatedEventHandler {

    private final ActiveRoutineRepository activeRoutineRepository;
    private final ActiveRoutineCommandService activeRoutineCommandService;

    public RoutineUpdatedEventHandler(ActiveRoutineRepository activeRoutineRepository,
                                      ActiveRoutineCommandService activeRoutineCommandService) {
        this.activeRoutineRepository = activeRoutineRepository;
        this.activeRoutineCommandService = activeRoutineCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(RoutineUpdated event) {
        activeRoutineRepository.findAllByRoutineId(new RoutineId(event.routineId()))
                .forEach(routine -> activeRoutineCommandService.handle(
                        new SyncActiveRoutineCommand(routine.getClientId())));
    }
}
