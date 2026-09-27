package com.formai.api.tracking.application.internal.outboundservices.acl;

import com.formai.api.clients.interfaces.acl.ClientsContextFacade;
import com.formai.api.tracking.domain.model.valueobjects.TrainerClient;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.shared.contracts.clients.ClientSummaryPage;
import com.formai.api.shared.contracts.clients.ClientListRequest;
import com.formai.api.planning.interfaces.acl.PlanningContextFacade;
import com.formai.api.shared.contracts.clients.ClientSummary;
import com.formai.api.shared.contracts.planning.ActiveRoutineSnapshot;
import com.formai.api.shared.contracts.planning.PrescribedExerciseSnapshot;
import com.formai.api.shared.contracts.planning.SessionSnapshot;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.ROUTINE_ID;
import static com.formai.api.tracking.TrackingTestData.START_DATE;
import static com.formai.api.tracking.TrackingTestData.TRAINER_HOLDER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalServicesTest {

    @Mock
    PlanningContextFacade planningContextFacade;

    @Mock
    ClientsContextFacade clientsContextFacade;

    @Test
    void shouldTranslatePlanningsSnapshotIntoAPlannedRoutine() {
        // Arrange
        var exerciseId = UUID.randomUUID();
        var snapshot = new ActiveRoutineSnapshot(CLIENT_ID.value(), ROUTINE_ID.value(), "Strength 12 weeks", 3,
                START_DATE, List.of(new SessionSnapshot(1, "Day A · Legs", List.of(
                        new PrescribedExerciseSnapshot(exerciseId, "Squat", 4, 8, new BigDecimal("70"), 120)))));
        when(planningContextFacade.fetchActiveRoutine(CLIENT_ID.value())).thenReturn(Optional.of(snapshot));

        // Act
        var plan = new ExternalPlanningService(planningContextFacade).fetchActiveRoutine(CLIENT_ID).orElseThrow();

        // Assert
        assertThat(plan.routineId()).isEqualTo(ROUTINE_ID);
        assertThat(plan.version()).isEqualTo(3);
        assertThat(plan.startDate()).isEqualTo(START_DATE);
        var exercise = plan.days().getFirst().exercises().getFirst();
        assertThat(exercise.exerciseId()).isEqualTo(new ExerciseId(exerciseId));
        assertThat(exercise.sets()).isEqualTo(4);
        assertThat(exercise.targetLoadKg()).isEqualByComparingTo("70");
    }

    @Test
    void shouldReturnEmptyWhenPlanningHasNoRoutineForTheClient() {
        when(planningContextFacade.fetchActiveRoutine(CLIENT_ID.value())).thenReturn(Optional.empty());

        assertThat(new ExternalPlanningService(planningContextFacade).fetchActiveRoutine(CLIENT_ID)).isEmpty();
    }

    @Test
    void shouldRecogniseAClientOfTheTrainer() {
        when(clientsContextFacade.fetchClientOfTrainer(CLIENT_ID.value(), TRAINER_HOLDER_ID))
                .thenReturn(Optional.of(new ClientSummary(CLIENT_ID.value(), "Luis Client", "luis@formai.com",
                        "ACTIVE")));

        assertThat(new ExternalClientsService(clientsContextFacade).isClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID))
                .isTrue();
    }

    @Test
    void shouldRejectAClientOfAnotherTrainer() {
        when(clientsContextFacade.fetchClientOfTrainer(CLIENT_ID.value(), TRAINER_HOLDER_ID))
                .thenReturn(Optional.empty());

        assertThat(new ExternalClientsService(clientsContextFacade).isClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID))
                .isFalse();
    }

    @Test
    void shouldTranslateTheTrainersClientPage() {
        when(clientsContextFacade.fetchClientsOfTrainer(new ClientListRequest(TRAINER_HOLDER_ID, Optional.of("lu"),
                Optional.empty(), 0, 20))).thenReturn(new ClientSummaryPage(List.of(
                new ClientSummary(CLIENT_ID.value(), "Luis Ramos", "luis@formai.com", "ACTIVE")), 0, 20, 1, 1));

        var page = new ExternalClientsService(clientsContextFacade).fetchClientsOfTrainer(TRAINER_HOLDER_ID,
                Optional.of("lu"), Optional.empty(), new Pagination(0, 20));

        assertThat(page.items()).containsExactly(new TrainerClient(CLIENT_ID, "Luis Ramos", "ACTIVE"));
        assertThat(page.totalElements()).isEqualTo(1);
    }
}
