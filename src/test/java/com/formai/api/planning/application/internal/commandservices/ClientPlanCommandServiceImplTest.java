package com.formai.api.planning.application.internal.commandservices;

import com.formai.api.planning.application.internal.outboundservices.acl.ExternalClientsService;
import com.formai.api.planning.domain.exceptions.AssigneeNotFoundException;
import com.formai.api.planning.domain.exceptions.ClientNotAssignableException;
import com.formai.api.planning.domain.exceptions.RoutineNotFoundException;
import com.formai.api.planning.domain.exceptions.TrainingDaysMismatchException;
import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.CloseAssignmentCommand;
import com.formai.api.planning.domain.model.commands.TransferClientPlanCommand;
import com.formai.api.planning.domain.model.events.AssignmentClosed;
import com.formai.api.planning.domain.model.events.RoutineAssigned;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;
import com.formai.api.planning.domain.repositories.ClientPlanRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.START_DATE;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.emptyPlan;
import static com.formai.api.planning.PlanningTestData.routine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientPlanCommandServiceImplTest {

    @Mock
    ClientPlanRepository clientPlanRepository;

    @Mock
    RoutineRepository routineRepository;

    @Mock
    ExternalClientsService externalClientsService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ClientPlanCommandServiceImpl commandService;

    private final RoutineId routineId = routine().getId();

    private AssignRoutineCommand assignFrom(LocalDate startDate) {
        return new AssignRoutineCommand(routineId, CLIENT_ID, TRAINER_HOLDER_ID, startDate, TWO_DAYS);
    }

    // routine() has two sessions, so it is assigned on two days of the week.
    private static final TrainingDays TWO_DAYS = TrainingDays.ofNames(List.of("MONDAY", "THURSDAY"));

    private void routineAndActiveClientExist() {
        when(routineRepository.findByIdAndHolderId(routineId, TRAINER_HOLDER_ID)).thenReturn(Optional.of(routine()));
        when(externalClientsService.isActiveClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(true));
        when(clientPlanRepository.save(any(ClientPlan.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void shouldAssignTheRoutineAndPublishRoutineAssigned() {
        // Arrange
        routineAndActiveClientExist();
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        // Act
        var plan = commandService.handle(assignFrom(START_DATE)).orElseThrow();

        // Assert
        assertThat(plan.currentAssignment()).hasValueSatisfying(assignment ->
                assertThat(assignment.getRoutineId()).isEqualTo(routineId));
        verify(eventPublisher).publishEvent(new RoutineAssigned(CLIENT_ID.value(), routineId.value(), START_DATE));
        verify(eventPublisher, never()).publishEvent(any(AssignmentClosed.class));
    }

    @Test
    void shouldCloseThePreviousAssignmentAndPublishAssignmentClosed() {
        var previousRoutine = new RoutineId(UUID.randomUUID());
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(previousRoutine, CLIENT_ID, TRAINER_HOLDER_ID, START_DATE, TrainingDays.everyDay()));
        routineAndActiveClientExist();
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(plan));

        commandService.handle(assignFrom(START_DATE.plusDays(28)));

        assertThat(plan.getAssignments()).hasSize(2);
        verify(eventPublisher).publishEvent(new AssignmentClosed(CLIENT_ID.value(), previousRoutine.value(),
                START_DATE.plusDays(27)));
        verify(eventPublisher).publishEvent(new RoutineAssigned(CLIENT_ID.value(), routineId.value(),
                START_DATE.plusDays(28)));
    }

    @Test
    void shouldRejectMoreTrainingDaysThanSessions() {
        when(routineRepository.findByIdAndHolderId(routineId, TRAINER_HOLDER_ID)).thenReturn(Optional.of(routine()));
        var threeDays = TrainingDays.ofNames(List.of("MONDAY", "WEDNESDAY", "FRIDAY"));

        assertThatThrownBy(() -> commandService.handle(
                new AssignRoutineCommand(routineId, CLIENT_ID, TRAINER_HOLDER_ID, START_DATE, threeDays)))
                .isInstanceOf(TrainingDaysMismatchException.class);
        verify(clientPlanRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldAnswerNotFoundForAnotherTrainersRoutine() {
        when(routineRepository.findByIdAndHolderId(routineId, TRAINER_HOLDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(assignFrom(START_DATE)))
                .isInstanceOf(RoutineNotFoundException.class);
    }

    @Test
    void shouldAnswerNotFoundForAClientThatIsNotTheTrainers() {
        when(routineRepository.findByIdAndHolderId(routineId, TRAINER_HOLDER_ID)).thenReturn(Optional.of(routine()));
        when(externalClientsService.isActiveClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(assignFrom(START_DATE)))
                .isInstanceOf(AssigneeNotFoundException.class);
    }

    @Test
    void shouldRejectAnInactiveClient() {
        when(routineRepository.findByIdAndHolderId(routineId, TRAINER_HOLDER_ID)).thenReturn(Optional.of(routine()));
        when(externalClientsService.isActiveClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(false));

        assertThatThrownBy(() -> commandService.handle(assignFrom(START_DATE)))
                .isInstanceOf(ClientNotAssignableException.class);
        verify(clientPlanRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldCloseTheCurrentAssignmentAndPublishAssignmentClosed() {
        var plan = emptyPlan();
        plan.assign(assignFrom(START_DATE));
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(plan));

        commandService.handle(new CloseAssignmentCommand(CLIENT_ID, START_DATE.plusDays(5)));

        assertThat(plan.currentAssignment()).isEmpty();
        verify(clientPlanRepository).save(plan);
        verify(eventPublisher).publishEvent(new AssignmentClosed(CLIENT_ID.value(), routineId.value(),
                START_DATE.plusDays(5)));
    }

    @Test
    void shouldIgnoreClosingForAClientWithoutAPlan() {
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        commandService.handle(new CloseAssignmentCommand(CLIENT_ID, START_DATE));

        verify(clientPlanRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldHandThePlanOverAndCloseItsAssignmentWhenTheClientChangesTrainer() {
        // Arrange
        var plan = emptyPlan();
        plan.assign(assignFrom(START_DATE));
        var newTrainer = "33333333-3333-3333-3333-333333333333";
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(plan));
        when(clientPlanRepository.save(any(ClientPlan.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        commandService.handle(new TransferClientPlanCommand(CLIENT_ID, newTrainer, START_DATE.plusDays(5)));

        // Assert
        assertThat(plan.getHolderId()).isEqualTo(newTrainer);
        assertThat(plan.currentAssignment()).isEmpty();
        verify(eventPublisher).publishEvent(new AssignmentClosed(CLIENT_ID.value(), routineId.value(),
                START_DATE.plusDays(4)));
    }

    @Test
    void shouldDoNothingWhenATransferredClientHasNoPlan() {
        when(clientPlanRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        commandService.handle(new TransferClientPlanCommand(CLIENT_ID, "33333333-3333-3333-3333-333333333333",
                START_DATE));

        verify(clientPlanRepository, never()).save(any(ClientPlan.class));
    }
}
