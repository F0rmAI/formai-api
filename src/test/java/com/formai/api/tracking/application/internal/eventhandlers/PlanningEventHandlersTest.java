package com.formai.api.tracking.application.internal.eventhandlers;

import com.formai.api.planning.domain.model.events.AssignmentClosed;
import com.formai.api.planning.domain.model.events.RoutineAssigned;
import com.formai.api.planning.domain.model.events.RoutineUpdated;
import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutinesOfRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.services.ActiveRoutineCommandService;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.ROUTINE_ID;
import static com.formai.api.tracking.TrackingTestData.START_DATE;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlanningEventHandlersTest {

    @Mock
    ActiveRoutineCommandService activeRoutineCommandService;

    @Mock
    WorkoutSessionCommandService workoutSessionCommandService;

    @Test
    void shouldSyncTheClientsRoutineWhenARoutineIsAssigned() {
        var handler = new RoutineAssignedEventHandler(activeRoutineCommandService, workoutSessionCommandService);

        handler.on(new RoutineAssigned(CLIENT_ID.value(), ROUTINE_ID.value(), START_DATE));

        verify(activeRoutineCommandService).handle(new SyncActiveRoutineCommand(CLIENT_ID));
    }

    @Test
    void shouldScheduleTodaysSessionWhenTheAssignedRoutineStartsToday() {
        var handler = new RoutineAssignedEventHandler(activeRoutineCommandService, workoutSessionCommandService);

        handler.scheduleOn(new RoutineAssigned(CLIENT_ID.value(), ROUTINE_ID.value(), TODAY), TODAY);

        verify(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY));
    }

    @Test
    void shouldNotScheduleASessionWhenTheAssignedRoutineStartsLater() {
        var handler = new RoutineAssignedEventHandler(activeRoutineCommandService, workoutSessionCommandService);

        handler.scheduleOn(new RoutineAssigned(CLIENT_ID.value(), ROUTINE_ID.value(), TODAY.plusDays(3)), TODAY);

        verify(workoutSessionCommandService, never()).handle(any(ScheduleWorkoutSessionCommand.class));
    }

    @Test
    void shouldNotFailTheAssignmentWhenTodaysSessionCannotBeScheduled() {
        doThrow(new ActiveRoutineNotFoundException())
                .when(workoutSessionCommandService).handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY));
        var handler = new RoutineAssignedEventHandler(activeRoutineCommandService, workoutSessionCommandService);

        assertThatCode(() -> handler.scheduleOn(new RoutineAssigned(CLIENT_ID.value(), ROUTINE_ID.value(), START_DATE),
                TODAY)).doesNotThrowAnyException();
    }

    @Test
    void shouldSyncEveryClientFollowingARoutineWhenItIsUpdated() {
        var handler = new RoutineUpdatedEventHandler(activeRoutineCommandService);

        handler.on(new RoutineUpdated(ROUTINE_ID.value(), 2));

        verify(activeRoutineCommandService).handle(new SyncActiveRoutinesOfRoutineCommand(ROUTINE_ID));
    }

    @Test
    void shouldEndTheClientsRoutineWhenTheAssignmentIsClosed() {
        var handler = new AssignmentClosedEventHandler(activeRoutineCommandService);

        handler.on(new AssignmentClosed(CLIENT_ID.value(), ROUTINE_ID.value(), TODAY));

        verify(activeRoutineCommandService).handle(new EndActiveRoutineCommand(CLIENT_ID, TODAY));
    }
}
