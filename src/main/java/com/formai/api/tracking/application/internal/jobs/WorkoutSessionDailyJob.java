package com.formai.api.tracking.application.internal.jobs;

import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.SkipOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesOnQuery;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class WorkoutSessionDailyJob {

    private static final Logger log = LoggerFactory.getLogger(WorkoutSessionDailyJob.class);

    private final ActiveRoutineQueryService activeRoutineQueryService;
    private final ActiveRoutineCommandService activeRoutineCommandService;
    private final WorkoutSessionCommandService workoutSessionCommandService;

    public WorkoutSessionDailyJob(ActiveRoutineQueryService activeRoutineQueryService,
                                  ActiveRoutineCommandService activeRoutineCommandService,
                                  WorkoutSessionCommandService workoutSessionCommandService) {
        this.activeRoutineQueryService = activeRoutineQueryService;
        this.activeRoutineCommandService = activeRoutineCommandService;
        this.workoutSessionCommandService = workoutSessionCommandService;
    }

    @Scheduled(cron = "${tracking.jobs.workout-session-daily.cron}")
    public void run() {
        runFor(LocalDate.now());
    }

    public void runFor(LocalDate today) {
        workoutSessionCommandService.handle(new SkipOverdueWorkoutSessionsCommand(today));
        for (var routine : activeRoutineQueryService.handle(new GetActiveRoutinesOnQuery(today))) {
            var clientId = routine.getClientId();
            try {
                var synced = activeRoutineCommandService.handle(new SyncActiveRoutineCommand(clientId));
                if (synced.isEmpty()) {
                    activeRoutineCommandService.handle(new EndActiveRoutineCommand(clientId, today.minusDays(1)));
                    continue;
                }
                if (synced.get().isActiveOn(today)) {
                    workoutSessionCommandService.handle(new ScheduleWorkoutSessionCommand(clientId, today));
                }
            } catch (RuntimeException ex) {
                log.error("Could not schedule today's workout session for client {}", clientId.value(), ex);
            }
        }
    }
}
