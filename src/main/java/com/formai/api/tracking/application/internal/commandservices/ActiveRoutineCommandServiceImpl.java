package com.formai.api.tracking.application.internal.commandservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalPlanningService;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ActiveRoutineCommandServiceImpl implements ActiveRoutineCommandService {

    private final ActiveRoutineRepository activeRoutineRepository;
    private final ExternalPlanningService externalPlanningService;

    public ActiveRoutineCommandServiceImpl(ActiveRoutineRepository activeRoutineRepository,
                                           ExternalPlanningService externalPlanningService) {
        this.activeRoutineRepository = activeRoutineRepository;
        this.externalPlanningService = externalPlanningService;
    }

    // Planning is the source of truth: the routine is always re-read from its facade, so
    // this is idempotent and also serves as the self-healing path when an event was missed.
    // Empty when planning has no routine assigned to the client.
    @Override
    public Optional<ActiveRoutine> handle(SyncActiveRoutineCommand command) {
        return externalPlanningService.fetchActiveRoutine(command.clientId())
                .map(plan -> {
                    var routine = activeRoutineRepository.findByClientId(command.clientId())
                            .map(existing -> {
                                existing.resync(plan);
                                return existing;
                            })
                            .orElseGet(() -> ActiveRoutine.syncFrom(command, plan));
                    return activeRoutineRepository.save(routine);
                });
    }

    // Closing an assignment and assigning a new one publish separate events whose order is
    // not guaranteed: if planning already reports a newer routine for the client, re-sync
    // to it instead of ending the one the client now follows.
    @Override
    public void handle(EndActiveRoutineCommand command) {
        activeRoutineRepository.findByClientId(command.clientId()).ifPresent(routine -> {
            externalPlanningService.fetchActiveRoutine(command.clientId())
                    .ifPresentOrElse(routine::resync, () -> routine.end(command));
            activeRoutineRepository.save(routine);
        });
    }
}
