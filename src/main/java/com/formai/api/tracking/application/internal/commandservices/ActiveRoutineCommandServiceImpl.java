package com.formai.api.tracking.application.internal.commandservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalPlanningService;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.commands.ChangeTimeZoneCommand;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.RecordDailyRunCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutinesOfRoutineCommand;
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

    // Every client following the routine gets its latest version.
    @Override
    public void handle(SyncActiveRoutinesOfRoutineCommand command) {
        activeRoutineRepository.findAllByRoutineId(command.routineId())
                .forEach(routine -> handle(new SyncActiveRoutineCommand(routine.getClientId())));
    }

    @Override
    public void handle(EndActiveRoutineCommand command) {
        activeRoutineRepository.findByClientId(command.clientId()).ifPresent(routine -> {
            externalPlanningService.fetchActiveRoutine(command.clientId())
                    .ifPresentOrElse(routine::resync, () -> routine.end(command));
            activeRoutineRepository.save(routine);
        });
    }

    // A client without a synced routine yet keeps the default zone until the next request.
    @Override
    public void handle(ChangeTimeZoneCommand command) {
        activeRoutineRepository.findByClientId(command.clientId())
                .filter(routine -> routine.changeTimeZone(command.timeZone()))
                .ifPresent(activeRoutineRepository::save);
    }

    @Override
    public void handle(RecordDailyRunCommand command) {
        activeRoutineRepository.findByClientId(command.clientId()).ifPresent(routine -> {
            routine.recordDailyRun(command.date());
            activeRoutineRepository.save(routine);
        });
    }
}
