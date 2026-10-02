package com.formai.api.planning.application;

import com.formai.api.clients.domain.model.events.ClientDeactivated;
import com.formai.api.clients.domain.model.events.ClientTransferred;
import com.formai.api.clients.interfaces.acl.ClientsContextFacade;
import com.formai.api.planning.application.acl.PlanningContextFacadeImpl;
import com.formai.api.planning.application.internal.eventhandlers.AssignmentClosedEventHandler;
import com.formai.api.planning.application.internal.eventhandlers.ClientDeactivatedEventHandler;
import com.formai.api.planning.application.internal.eventhandlers.ClientTransferredEventHandler;
import com.formai.api.planning.application.internal.eventhandlers.RoutineAssignedEventHandler;
import com.formai.api.planning.application.internal.outboundservices.acl.ExternalClientsService;
import com.formai.api.planning.domain.model.commands.CloseAssignmentCommand;
import com.formai.api.planning.domain.model.commands.CloseRoutineCommand;
import com.formai.api.planning.domain.model.commands.MarkRoutineActiveCommand;
import com.formai.api.planning.domain.model.commands.TransferClientPlanCommand;
import com.formai.api.planning.domain.model.events.AssignmentClosed;
import com.formai.api.planning.domain.model.events.RoutineAssigned;
import com.formai.api.planning.domain.model.queries.GetActiveAssignmentByClientIdQuery;
import com.formai.api.planning.domain.model.valueobjects.ActiveAssignment;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;
import com.formai.api.planning.domain.services.ClientPlanCommandService;
import com.formai.api.planning.domain.services.ClientPlanQueryService;
import com.formai.api.planning.domain.services.RoutineCommandService;
import com.formai.api.shared.contracts.clients.ClientSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.START_DATE;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.routine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningIntegrationPointsTest {

    @Mock
    ClientPlanQueryService clientPlanQueryService;

    @Mock
    ClientPlanCommandService clientPlanCommandService;

    @Mock
    RoutineCommandService routineCommandService;

    @Mock
    ClientsContextFacade clientsContextFacade;

    @Test
    void shouldPublishTheCurrentRoutineVersionThroughTheFacade() {
        // Arrange
        var routine = routine();
        when(clientPlanQueryService.handle(new GetActiveAssignmentByClientIdQuery(CLIENT_ID)))
                .thenReturn(Optional.of(new ActiveAssignment(CLIENT_ID, routine, START_DATE, TrainingDays.everyDay())));

        // Act
        var snapshot = new PlanningContextFacadeImpl(clientPlanQueryService).fetchActiveRoutine(CLIENT_ID.value())
                .orElseThrow();

        // Assert
        assertThat(snapshot.routineId()).isEqualTo(routine.getId().value());
        assertThat(snapshot.version()).isEqualTo(1);
        assertThat(snapshot.startDate()).isEqualTo(START_DATE);
        assertThat(snapshot.sessions()).hasSize(2);
        var squat = snapshot.sessions().getFirst().exercises().getFirst();
        assertThat(squat.exerciseName()).isEqualTo("Squat");
        assertThat(squat.sets()).isEqualTo(3);
        assertThat(squat.targetLoadKg()).isEqualByComparingTo("60");
    }

    @Test
    void shouldPublishNothingForAClientWithoutARoutine() {
        when(clientPlanQueryService.handle(new GetActiveAssignmentByClientIdQuery(CLIENT_ID))).thenReturn(Optional.empty());

        assertThat(new PlanningContextFacadeImpl(clientPlanQueryService).fetchActiveRoutine(CLIENT_ID.value())).isEmpty();
    }

    @Test
    void shouldTellWhetherTheTrainersClientIsActive() {
        var acl = new ExternalClientsService(clientsContextFacade);
        when(clientsContextFacade.fetchClientOfTrainer(CLIENT_ID.value(), TRAINER_HOLDER_ID))
                .thenReturn(Optional.of(new ClientSummary(CLIENT_ID.value(), "Luis", "luis@formai.com", "ACTIVE")))
                .thenReturn(Optional.of(new ClientSummary(CLIENT_ID.value(), "Luis", "luis@formai.com", "INACTIVE")))
                .thenReturn(Optional.empty());

        assertThat(acl.isActiveClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).contains(true);
        assertThat(acl.isActiveClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).contains(false);
        assertThat(acl.isActiveClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).isEmpty();
    }

    @Test
    void shouldMarkTheRoutineActiveOnceItIsAssigned() {
        var routine = routine();

        new RoutineAssignedEventHandler(routineCommandService)
                .on(new RoutineAssigned(CLIENT_ID.value(), routine.getId().value(), START_DATE));

        verify(routineCommandService).handle(new MarkRoutineActiveCommand(routine.getId()));
    }

    @Test
    void shouldCloseTheAssignmentOfADeactivatedClientToday() {
        new ClientDeactivatedEventHandler(clientPlanCommandService)
                .on(new ClientDeactivated(CLIENT_ID.value(), TRAINER_HOLDER_ID));

        verify(clientPlanCommandService).handle(new CloseAssignmentCommand(CLIENT_ID, LocalDate.now()));
    }

    @Test
    void shouldTryToCloseTheRoutineWhenAnAssignmentCloses() {
        var routine = routine();

        new AssignmentClosedEventHandler(routineCommandService)
                .on(new AssignmentClosed(CLIENT_ID.value(), routine.getId().value(), START_DATE));

        verify(routineCommandService).handle(new CloseRoutineCommand(routine.getId()));
    }

    @Test
    void shouldHandThePlanOverTodayWhenAClientChangesTrainer() {
        var newTrainer = "33333333-3333-3333-3333-333333333333";

        new ClientTransferredEventHandler(clientPlanCommandService)
                .on(new ClientTransferred(CLIENT_ID.value(), TRAINER_HOLDER_ID, newTrainer));

        verify(clientPlanCommandService).handle(new TransferClientPlanCommand(CLIENT_ID, newTrainer, LocalDate.now()));
    }
}
