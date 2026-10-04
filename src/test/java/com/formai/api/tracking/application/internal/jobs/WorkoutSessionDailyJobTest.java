package com.formai.api.tracking.application.internal.jobs;

import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.commands.CloseClientOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.commands.CloseOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.RecordDailyRunCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesAtQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.PlannedRoutine;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.ROUTINE_ID;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionDailyJobTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    // 10:00 in Lima on TODAY, the default zone of the fixtures.
    private static final Instant NOW = TODAY.atTime(10, 0).atZone(LIMA).toInstant();

    @Mock
    ActiveRoutineQueryService activeRoutineQueryService;

    @Mock
    ActiveRoutineCommandService activeRoutineCommandService;

    @Mock
    WorkoutSessionCommandService workoutSessionCommandService;

    @InjectMocks
    WorkoutSessionDailyJob job;

    private static ActiveRoutine routineOf(ClientId clientId) {
        var routine = activeRoutine();
        routine.setClientId(clientId);
        return routine;
    }

    @Test
    void shouldCloseThePreviousDaysScheduleTodaysSessionAndRecordTheRun() {
        // Arrange
        var routine = activeRoutine();
        when(activeRoutineQueryService.handle(new GetActiveRoutinesAtQuery(NOW))).thenReturn(List.of(routine));
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(CLIENT_ID))).thenReturn(Optional.of(routine));

        // Act
        job.runAt(NOW);

        // Assert
        InOrder order = inOrder(workoutSessionCommandService, activeRoutineCommandService);
        order.verify(workoutSessionCommandService).handle(new CloseClientOverdueWorkoutSessionsCommand(CLIENT_ID, TODAY));
        order.verify(activeRoutineCommandService).handle(new SyncActiveRoutineCommand(CLIENT_ID));
        order.verify(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY));
        order.verify(activeRoutineCommandService).handle(new RecordDailyRunCommand(CLIENT_ID, TODAY));
    }

    @Test
    void shouldStartEachClientsDayAtTheirOwnMidnight() {
        // 00:30 in Buenos Aires on the day after TODAY is still 22:30 of TODAY in Lima.
        var instant = TODAY.plusDays(1).atTime(LocalTime.of(0, 30)).atZone(BUENOS_AIRES).toInstant();
        var inLima = activeRoutine();
        inLima.recordDailyRun(TODAY);
        var porteno = new ClientId(UUID.randomUUID());
        var inBuenosAires = routineOf(porteno);
        inBuenosAires.changeTimeZone(BUENOS_AIRES);
        inBuenosAires.recordDailyRun(TODAY);
        when(activeRoutineQueryService.handle(new GetActiveRoutinesAtQuery(instant)))
                .thenReturn(List.of(inLima, inBuenosAires));
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(porteno)))
                .thenReturn(Optional.of(inBuenosAires));

        job.runAt(instant);

        verify(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(porteno, TODAY.plusDays(1)));
        verify(activeRoutineCommandService, never()).handle(new SyncActiveRoutineCommand(CLIENT_ID));
    }

    @Test
    void shouldProcessEachClientOnlyOncePerDay() {
        var routine = activeRoutine();
        routine.recordDailyRun(TODAY);
        when(activeRoutineQueryService.handle(new GetActiveRoutinesAtQuery(NOW))).thenReturn(List.of(routine));

        job.runAt(NOW);

        verify(activeRoutineCommandService, never()).handle(any(SyncActiveRoutineCommand.class));
        verify(workoutSessionCommandService, never()).handle(any(ScheduleWorkoutSessionCommand.class));
    }

    @Test
    void shouldCloseSessionsOfEndedRoutinesOnceTheDateIsOverEverywhere() {
        when(activeRoutineQueryService.handle(new GetActiveRoutinesAtQuery(NOW))).thenReturn(List.of());

        job.runAt(NOW);

        // 10:00 in Lima is 15:00 UTC, which at UTC-18:00 is still 21:00 of the day before TODAY.
        verify(workoutSessionCommandService).handle(new CloseOverdueWorkoutSessionsCommand(TODAY.minusDays(1)));
    }

    @Test
    void shouldEndTheRoutineAndScheduleNothingWhenPlanningNoLongerAssignsIt() {
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(CLIENT_ID))).thenReturn(Optional.empty());

        job.runFor(CLIENT_ID, TODAY);

        verify(activeRoutineCommandService).handle(new EndActiveRoutineCommand(CLIENT_ID, TODAY.minusDays(1)));
        verify(workoutSessionCommandService, never()).handle(any(ScheduleWorkoutSessionCommand.class));
    }

    @Test
    void shouldNotScheduleARoutineBeforeItsStartDate() {
        var startsLater = activeRoutine();
        startsLater.resync(new PlannedRoutine(ROUTINE_ID, "Next block", 1, TODAY.plusDays(3), EnumSet.allOf(DayOfWeek.class),
                startsLater.getDays()));
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(CLIENT_ID)))
                .thenReturn(Optional.of(startsLater));

        job.runFor(CLIENT_ID, TODAY);

        verify(workoutSessionCommandService, never()).handle(any(ScheduleWorkoutSessionCommand.class));
    }

    @Test
    void shouldKeepSchedulingOtherClientsAndRetryTheFailedOne() {
        var failing = new ClientId(UUID.randomUUID());
        var failingRoutine = routineOf(failing);
        var routine = activeRoutine();
        when(activeRoutineQueryService.handle(new GetActiveRoutinesAtQuery(NOW)))
                .thenReturn(List.of(failingRoutine, routine));
        doReturn(Optional.of(failingRoutine))
                .when(activeRoutineCommandService).handle(new SyncActiveRoutineCommand(failing));
        doReturn(Optional.of(routine))
                .when(activeRoutineCommandService).handle(new SyncActiveRoutineCommand(CLIENT_ID));
        lenient().doThrow(new ActiveRoutineNotFoundException())
                .when(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(failing, TODAY));

        job.runAt(NOW);

        verify(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY));
        // Not recorded, so the next run tries that client's day again.
        verify(activeRoutineCommandService, never()).handle(new RecordDailyRunCommand(failing, TODAY));
    }

    @Test
    void shouldRunTheJobOnStartupToCatchUpAMissedCron() {
        job.runOnStartup();

        verify(workoutSessionCommandService).handle(any(CloseOverdueWorkoutSessionsCommand.class));
    }

    @Test
    void shouldNotStopTheApplicationWhenTheStartupCatchUpFails() {
        doThrow(new IllegalStateException("database unavailable"))
                .when(workoutSessionCommandService).handle(any(CloseOverdueWorkoutSessionsCommand.class));

        assertThatCode(() -> job.runOnStartup()).doesNotThrowAnyException();
    }

    @Test
    void shouldNotScheduleOnARestDayButStillRecordTheRun() {
        var routine = activeRoutine();
        routine.setTrainingDays(EnumSet.of(TODAY.plusDays(1).getDayOfWeek()));
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(CLIENT_ID))).thenReturn(Optional.of(routine));

        job.runFor(CLIENT_ID, TODAY);

        verify(workoutSessionCommandService, never()).handle(any(ScheduleWorkoutSessionCommand.class));
        verify(activeRoutineCommandService).handle(new RecordDailyRunCommand(CLIENT_ID, TODAY));
    }
}
