package com.formai.api.tracking.application.internal.queryservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalClientsService;
import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.model.queries.GetWorkoutHistoryQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutSessionByIdQuery;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.formai.api.tracking.TrackingTestData.CLIENT_HOLDER_ID;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.TRAINER_HOLDER_ID;
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
}
