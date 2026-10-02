package com.formai.api.tracking.application.internal.queryservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalClientsService;
import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.model.queries.GetClientOverviewsQuery;
import com.formai.api.tracking.domain.model.queries.GetExerciseProgressQuery;
import com.formai.api.tracking.domain.model.queries.GetProgressReportQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutHistoryQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutSessionByIdQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.LastWorkout;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.ProgressWindow;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.domain.model.valueobjects.TrainerClient;
import com.formai.api.tracking.domain.model.valueobjects.TrainerClientPage;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.formai.api.tracking.TrackingTestData.CLIENT_HOLDER_ID;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.SQUAT;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.TRAINER_HOLDER_ID;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static com.formai.api.tracking.TrackingTestData.pendingSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionQueryServiceImplTest {

    private static final Pagination FIRST_PAGE = new Pagination(0, 20);

    @Mock
    WorkoutSessionRepository workoutSessionRepository;

    @Mock
    ActiveRoutineRepository activeRoutineRepository;

    @Mock
    ExternalClientsService externalClientsService;

    @InjectMocks
    WorkoutSessionQueryServiceImpl queryService;

    private static WorkoutSessionPage pageOf(int sessions) {
        var items = IntStream.range(0, sessions)
                .mapToObj(day -> pendingSession(TODAY.minusDays(day)))
                .toList();
        return new WorkoutSessionPage(items, 0, 20, sessions, 1);
    }

    @Test
    void shouldReturnTheClientsOwnHistoryWithoutAskingClients() {
        // Arrange
        var page = pageOf(2);
        when(workoutSessionRepository.findAllByClientId(CLIENT_ID, Optional.empty(), FIRST_PAGE)).thenReturn(page);

        // Act
        var result = queryService.handle(new GetWorkoutHistoryQuery(CLIENT_ID, CLIENT_HOLDER_ID, Optional.empty(),
                FIRST_PAGE));

        // Assert
        assertThat(result).isSameAs(page);
        verifyNoInteractions(externalClientsService);
    }

    @Test
    void shouldLetTheClientsTrainerReadTheHistoryFilteredByPeriod() {
        var period = Optional.of(new ReportPeriod(TODAY.minusDays(7), TODAY));
        var page = pageOf(1);
        when(externalClientsService.isClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(true);
        when(workoutSessionRepository.findAllByClientId(CLIENT_ID, period, FIRST_PAGE)).thenReturn(page);

        var result = queryService.handle(new GetWorkoutHistoryQuery(CLIENT_ID, TRAINER_HOLDER_ID, period, FIRST_PAGE));

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void shouldDenyTheHistoryToSomeoneWhoIsNotTheClientsTrainer() {
        when(externalClientsService.isClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(false);

        assertThatThrownBy(() -> queryService.handle(new GetWorkoutHistoryQuery(CLIENT_ID, TRAINER_HOLDER_ID,
                Optional.empty(), FIRST_PAGE)))
                .isInstanceOf(ClientAccessDeniedException.class);
        verify(workoutSessionRepository, never()).findAllByClientId(any(), any(), any());
    }

    @Test
    void shouldReturnOneOfTheClientsOwnSessions() {
        var session = pendingSession(TODAY);
        when(workoutSessionRepository.findByIdAndClientId(session.getId(), CLIENT_ID)).thenReturn(Optional.of(session));

        var result = queryService.handle(new GetWorkoutSessionByIdQuery(session.getId(), CLIENT_ID, CLIENT_HOLDER_ID));

        assertThat(result).containsSame(session);
    }

    @Test
    void shouldReturnEmptyForASessionOfAnotherClient() {
        var session = pendingSession(TODAY);
        when(workoutSessionRepository.findByIdAndClientId(session.getId(), CLIENT_ID)).thenReturn(Optional.empty());

        assertThat(queryService.handle(new GetWorkoutSessionByIdQuery(session.getId(), CLIENT_ID, CLIENT_HOLDER_ID)))
                .isEmpty();
    }

    @Test
    void shouldListNothingWhenTheClientHasNoSessions() {
        var empty = new WorkoutSessionPage(List.of(), 0, 20, 0, 0);
        when(workoutSessionRepository.findAllByClientId(CLIENT_ID, Optional.empty(), FIRST_PAGE)).thenReturn(empty);

        var result = queryService.handle(new GetWorkoutHistoryQuery(CLIENT_ID, CLIENT_HOLDER_ID, Optional.empty(),
                FIRST_PAGE));

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void shouldAddTheCurrentRoutineAndTheLastWorkoutToEachClient() {
        var other = new ClientId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        when(externalClientsService.fetchClientsOfTrainer(TRAINER_HOLDER_ID, Optional.empty(), Optional.of("ACTIVE"),
                FIRST_PAGE)).thenReturn(new TrainerClientPage(List.of(
                        new TrainerClient(CLIENT_ID, "Luis Ramos", "ACTIVE"),
                        new TrainerClient(other, "Maria Diaz", "ACTIVE")), 0, 20, 2, 1));
        when(activeRoutineRepository.findAllByClientIds(List.of(CLIENT_ID, other))).thenReturn(List.of(activeRoutine()));
        when(workoutSessionRepository.findLastWorkoutDates(List.of(CLIENT_ID, other)))
                .thenReturn(List.of(new LastWorkout(CLIENT_ID, TODAY.minusDays(2))));

        var page = queryService.handle(new GetClientOverviewsQuery(TRAINER_HOLDER_ID, Optional.empty(),
                Optional.of("ACTIVE"), FIRST_PAGE));

        assertThat(page.items()).hasSize(2);
        var luis = page.items().getFirst();
        assertThat(luis.activeRoutineName()).contains(activeRoutine().getRoutineName());
        assertThat(luis.lastWorkoutOn()).contains(TODAY.minusDays(2));
        var maria = page.items().getLast();
        assertThat(maria.activeRoutineName()).isEmpty();
        assertThat(maria.lastWorkoutOn()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldNotReadTrackingDataForATrainerWithoutClients() {
        when(externalClientsService.fetchClientsOfTrainer(TRAINER_HOLDER_ID, Optional.empty(), Optional.empty(),
                FIRST_PAGE)).thenReturn(new TrainerClientPage(List.of(), 0, 20, 0, 0));

        var page = queryService.handle(new GetClientOverviewsQuery(TRAINER_HOLDER_ID, Optional.empty(),
                Optional.empty(), FIRST_PAGE));

        assertThat(page.items()).isEmpty();
        verifyNoInteractions(activeRoutineRepository, workoutSessionRepository);
    }

    @Test
    void shouldReportTheProgressOfTheTrainersClient() {
        var period = new ReportPeriod(TODAY.minusDays(27), TODAY);
        when(externalClientsService.isClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(true);
        when(workoutSessionRepository.findAllByClientIdAndPeriod(CLIENT_ID, period)).thenReturn(List.of(pendingSession(TODAY)));

        var report = queryService.handle(new GetProgressReportQuery(CLIENT_ID, TRAINER_HOLDER_ID, period));

        assertThat(report.scheduled()).isEqualTo(1);
        assertThat(report.hasData()).isFalse();
    }

    @Test
    void shouldDenyTheProgressOfAnotherTrainersClient() {
        when(externalClientsService.isClientOfTrainer(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(false);

        assertThatThrownBy(() -> queryService.handle(new GetProgressReportQuery(CLIENT_ID, TRAINER_HOLDER_ID,
                new ReportPeriod(TODAY.minusDays(27), TODAY))))
                .isInstanceOf(ClientAccessDeniedException.class);
        verifyNoInteractions(workoutSessionRepository);
    }

    @Test
    void shouldChartTheClientsOwnExerciseOverTheChosenWeeks() {
        var squat = SQUAT.exerciseId();
        when(workoutSessionRepository.findAllByClientIdAndExerciseIdSince(CLIENT_ID, squat,
                LocalDate.now().minusWeeks(8))).thenReturn(List.of());

        var progress = queryService.handle(new GetExerciseProgressQuery(CLIENT_ID, CLIENT_HOLDER_ID, squat,
                ProgressWindow.WEEKS_8));

        assertThat(progress.window()).isEqualTo(ProgressWindow.WEEKS_8);
        assertThat(progress.enoughData()).isFalse();
        verifyNoInteractions(externalClientsService);
    }
}
