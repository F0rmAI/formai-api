package com.formai.api.tracking.application.internal.queryservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalPlanningService;
import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutineQuery;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutinesOnQuery;
import com.formai.api.tracking.domain.model.valueobjects.PlannedRoutine;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.ROUTINE_ID;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static com.formai.api.tracking.TrackingTestData.pendingSession;
import static com.formai.api.tracking.TrackingTestData.plannedRoutine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveRoutineQueryServiceImplTest {

    @Mock
    ActiveRoutineRepository activeRoutineRepository;

    @Mock
    WorkoutSessionRepository workoutSessionRepository;

    @Mock
    ExternalPlanningService externalPlanningService;

    @InjectMocks
    ActiveRoutineQueryServiceImpl queryService;

    @Test
    void shouldReturnTheRoutineWithTodaysSession() {
        // Arrange
        var routine = activeRoutine();
        var today = pendingSession(TODAY);
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(routine));
        when(workoutSessionRepository.findByClientIdAndScheduledFor(CLIENT_ID, TODAY))
                .thenReturn(Optional.of(today));

        // Act
        var plan = queryService.handle(new GetActiveRoutineQuery(CLIENT_ID, TODAY)).orElseThrow();

        // Assert
        assertThat(plan.routine()).isSameAs(routine);
        assertThat(plan.todaySession()).containsSame(today);
    }

    @Test
    void shouldShowPlanningsRoutineWhenTheLocalCopyIsMissing() {
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());
        when(externalPlanningService.fetchActiveRoutine(CLIENT_ID))
                .thenReturn(Optional.of(plannedRoutine(4)));
        when(workoutSessionRepository.findByClientIdAndScheduledFor(CLIENT_ID, TODAY))
                .thenReturn(Optional.empty());

        var plan = queryService.handle(new GetActiveRoutineQuery(CLIENT_ID, TODAY)).orElseThrow();

        assertThat(plan.routine().getVersion()).isEqualTo(4);
        assertThat(plan.todaySession()).isEmpty();
        verify(activeRoutineRepository, never()).save(any());
    }

    @Test
    void shouldFallBackToPlanningWhenTheLocalCopyHasEnded() {
        var ended = activeRoutine();
        ended.end(new EndActiveRoutineCommand(CLIENT_ID, TODAY.minusDays(1)));
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(ended));
        when(externalPlanningService.fetchActiveRoutine(CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.handle(new GetActiveRoutineQuery(CLIENT_ID, TODAY)))
                .isInstanceOf(ActiveRoutineNotFoundException.class);
    }

    @Test
    void shouldNotShowARoutineBeforeItsStartDate() {
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());
        when(externalPlanningService.fetchActiveRoutine(CLIENT_ID)).thenReturn(Optional.of(
                new PlannedRoutine(ROUTINE_ID, "Next block", 1, TODAY.plusDays(3), EnumSet.allOf(DayOfWeek.class), plannedRoutine(1).days())));

        assertThatThrownBy(() -> queryService.handle(new GetActiveRoutineQuery(CLIENT_ID, TODAY)))
                .isInstanceOf(ActiveRoutineNotFoundException.class);
    }

    @Test
    void shouldListTheRoutinesActiveOnADate() {
        var routine = activeRoutine();
        when(activeRoutineRepository.findAllActiveOn(TODAY)).thenReturn(List.of(routine));

        assertThat(queryService.handle(new GetActiveRoutinesOnQuery(TODAY))).containsExactly(routine);
    }
}
