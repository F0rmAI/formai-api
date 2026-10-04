package com.formai.api.tracking.application.internal.jobs;

import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.CloseClientOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.commands.CloseOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.commands.RecordDailyRunCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesAtQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

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

    // Runs every few minutes: each client's day starts at midnight in their own time zone, so the
    // job processes every client right after their midnight, once per day (lastDailyRunOn).
    @Scheduled(cron = "${tracking.jobs.workout-session-daily.cron}")
    public void run() {
        runAt(Instant.now());
    }

    // Catch-up: if the application was down when the cron fired, today's sessions still get
    // scheduled. runAt is idempotent, so running it again on the same day changes nothing.
    // A failure here must not stop the application: Spring Boot aborts startup on it.
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        try {
            run();
        } catch (RuntimeException ex) {
            log.error("Could not run the workout session catch-up on startup", ex);
        }
    }

    public void runAt(Instant now) {
        // Sessions of clients without a current routine: closed once that date is over in every zone.
        workoutSessionCommandService.handle(new CloseOverdueWorkoutSessionsCommand(
                LocalDate.ofInstant(now, ZoneOffset.MIN)));
        for (var routine : activeRoutineQueryService.handle(new GetActiveRoutinesAtQuery(now))) {
            var today = routine.today(now);
            if (!routine.isDueForDailyRun(today)) {
                continue;
            }
            var clientId = routine.getClientId();
            try {
                runFor(clientId, today);
            } catch (RuntimeException ex) {
                log.error("Could not schedule today's workout session for client {}", clientId.value(), ex);
            }
        }
    }

    // today: the client's own date. A failure leaves the day unrecorded, so the next run retries it.
    public void runFor(ClientId clientId, LocalDate today) {
        workoutSessionCommandService.handle(new CloseClientOverdueWorkoutSessionsCommand(clientId, today));
        var synced = activeRoutineCommandService.handle(new SyncActiveRoutineCommand(clientId));
        if (synced.isEmpty()) {
            activeRoutineCommandService.handle(new EndActiveRoutineCommand(clientId, today.minusDays(1)));
            return;
        }
        if (synced.get().trainsOn(today)) {
            workoutSessionCommandService.handle(new ScheduleWorkoutSessionCommand(clientId, today));
        }
        activeRoutineCommandService.handle(new RecordDailyRunCommand(clientId, today));
    }
}
