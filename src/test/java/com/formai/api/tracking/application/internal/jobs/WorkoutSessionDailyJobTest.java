package com.formai.api.tracking.application.internal.jobs;

import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.CloseOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesOnQuery;
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
    void shouldCloseThePreviousDaysAndScheduleTodaysSession() {
        // Arrange
        var routine = activeRoutine();
        when(activeRoutineQueryService.handle(new GetActiveRoutinesOnQuery(TODAY))).thenReturn(List.of(routine));
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(CLIENT_ID))).thenReturn(Optional.of(routine));

        // Act
        job.runFor(TODAY);

        // Assert
        InOrder order = inOrder(workoutSessionCommandService, activeRoutineCommandService);
        order.verify(workoutSessionCommandService).handle(new CloseOverdueWorkoutSessionsCommand(TODAY));
        order.verify(activeRoutineCommandService).handle(new SyncActiveRoutineCommand(CLIENT_ID));
        order.verify(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY));
    }

    @Test
    void shouldEndTheRoutineAndScheduleNothingWhenPlanningNoLongerAssignsIt() {
        when(activeRoutineQueryService.handle(new GetActiveRoutinesOnQuery(TODAY))).thenReturn(List.of(activeRoutine()));
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(CLIENT_ID))).thenReturn(Optional.empty());

        job.runFor(TODAY);

        verify(activeRoutineCommandService).handle(new EndActiveRoutineCommand(CLIENT_ID, TODAY.minusDays(1)));
        verify(workoutSessionCommandService, never()).handle(any(ScheduleWorkoutSessionCommand.class));
    }

    @Test
    void shouldNotScheduleARoutineBeforeItsStartDate() {
        var startsLater = activeRoutine();
        startsLater.resync(new PlannedRoutine(ROUTINE_ID, "Next block", 1, TODAY.plusDays(3), EnumSet.allOf(DayOfWeek.class),
                startsLater.getDays()));
        when(activeRoutineQueryService.handle(new GetActiveRoutinesOnQuery(TODAY))).thenReturn(List.of(activeRoutine()));
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(CLIENT_ID)))
                .thenReturn(Optional.of(startsLater));

        job.runFor(TODAY);

        verify(workoutSessionCommandService, never()).handle(any(ScheduleWorkoutSessionCommand.class));
    }

    @Test
    void shouldKeepSchedulingOtherClientsWhenOneFails() {
        var failing = new ClientId(UUID.randomUUID());
        var failingRoutine = routineOf(failing);
        var routine = activeRoutine();
        when(activeRoutineQueryService.handle(new GetActiveRoutinesOnQuery(TODAY)))
                .thenReturn(List.of(failingRoutine, routine));
        doReturn(Optional.of(failingRoutine))
                .when(activeRoutineCommandService).handle(new SyncActiveRoutineCommand(failing));
        doReturn(Optional.of(routine))
                .when(activeRoutineCommandService).handle(new SyncActiveRoutineCommand(CLIENT_ID));
        lenient().doThrow(new ActiveRoutineNotFoundException())
                .when(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(failing, TODAY));

        job.runFor(TODAY);

        verify(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY));
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
    void shouldNotScheduleOnARestDay() {
        var routine = activeRoutine();
        routine.setTrainingDays(EnumSet.of(TODAY.plusDays(1).getDayOfWeek()));
        when(activeRoutineQueryService.handle(new GetActiveRoutinesOnQuery(TODAY))).thenReturn(List.of(routine));
        when(activeRoutineCommandService.handle(new SyncActiveRoutineCommand(CLIENT_ID))).thenReturn(Optional.of(routine));

        job.runFor(TODAY);

        verify(workoutSessionCommandService, never()).handle(any(ScheduleWorkoutSessionCommand.class));
    }
}
