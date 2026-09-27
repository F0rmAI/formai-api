package com.formai.api.planning.application.internal.queryservices;

import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;
import com.formai.api.planning.domain.model.queries.GetActiveAssignmentByClientIdQuery;
import com.formai.api.planning.domain.model.queries.GetRoutineVersionsQuery;
import com.formai.api.planning.domain.repositories.ClientPlanRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.START_DATE;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.emptyPlan;
import static com.formai.api.planning.PlanningTestData.routine;
import static com.formai.api.planning.PlanningTestData.twoSessions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// The planning queries with behaviour of their own: version history and the active
// assignment tracking reads through the facade.
@ExtendWith(MockitoExtension.class)
class PlanningQueryServicesTest {

    @Mock
    RoutineRepository routineRepository;

    @Mock
    ClientPlanRepository clientPlanRepository;

    @Test
    void shouldListTheVersionsMostRecentFirst() {
        // Arrange
        var routine = routine();
        routine.revise(new UpdateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID, routine.getName(), twoSessions()));
        when(routineRepository.findByIdAndHolderId(routine.getId(), TRAINER_HOLDER_ID)).thenReturn(Optional.of(routine));

        // Act
        var versions = new RoutineQueryServiceImpl(routineRepository)
                .handle(new GetRoutineVersionsQuery(routine.getId(), TRAINER_HOLDER_ID));

        // Assert
        assertThat(versions).extracting(version -> version.getNumber()).containsExactly(2, 1);
    }

    @Test
    void shouldReturnNoVersionsForAnotherTrainersRoutine() {
        var routine = routine();
        when(routineRepository.findByIdAndHolderId(routine.getId(), TRAINER_HOLDER_ID)).thenReturn(Optional.empty());

        assertThat(new RoutineQueryServiceImpl(routineRepository)
                .handle(new GetRoutineVersionsQuery(routine.getId(), TRAINER_HOLDER_ID))).isEmpty();
    }

    @Test
    void shouldReturnTheCurrentAssignmentWithItsRoutine() {
        var routine = routine();
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(routine.getId(), CLIENT_ID, TRAINER_HOLDER_ID, START_DATE));
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(plan));
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        var active = new ClientPlanQueryServiceImpl(clientPlanRepository, routineRepository)
                .handle(new GetActiveAssignmentByClientIdQuery(CLIENT_ID)).orElseThrow();

        assertThat(active.routine()).isSameAs(routine);
        assertThat(active.startDate()).isEqualTo(START_DATE);
    }

    @Test
    void shouldKeepServingThePreviousRoutineUntilTheNewOneStarts() {
        var today = LocalDate.now();
        var previous = routine();
        var next = routine();
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(previous.getId(), CLIENT_ID, TRAINER_HOLDER_ID, today.minusDays(10)));
        plan.assign(new AssignRoutineCommand(next.getId(), CLIENT_ID, TRAINER_HOLDER_ID, today.plusDays(5)));
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(plan));
        when(routineRepository.findById(previous.getId())).thenReturn(Optional.of(previous));

        var active = new ClientPlanQueryServiceImpl(clientPlanRepository, routineRepository)
                .handle(new GetActiveAssignmentByClientIdQuery(CLIENT_ID)).orElseThrow();

        assertThat(active.routine()).isSameAs(previous);
        assertThat(active.startDate()).isEqualTo(today.minusDays(10));
    }

    @Test
    void shouldReturnNoActiveAssignmentOnceItIsClosed() {
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(routine().getId(), CLIENT_ID, TRAINER_HOLDER_ID, START_DATE));
        plan.closeCurrentAssignment(START_DATE.plusDays(3));
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(plan));

        assertThat(new ClientPlanQueryServiceImpl(clientPlanRepository, routineRepository)
                .handle(new GetActiveAssignmentByClientIdQuery(CLIENT_ID))).isEmpty();
    }
}
