package com.formai.api.tracking.application.internal.commandservices;

import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionNotFoundException;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.commands.CorrectSetCommand;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.CloseOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.events.SetRecorded;
import com.formai.api.tracking.domain.model.events.WorkoutSessionFinished;
import com.formai.api.tracking.domain.model.events.WorkoutSessionScheduled;
import com.formai.api.tracking.domain.model.events.WorkoutSessionSkipped;
import com.formai.api.tracking.domain.model.valueobjects.ComplianceStatus;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.tracking.TrackingTestData.BACK_DAY;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.LEGS_DAY;
import static com.formai.api.tracking.TrackingTestData.SQUAT;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static com.formai.api.tracking.TrackingTestData.pendingSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionCommandServiceImplTest {

    @Mock
    WorkoutSessionRepository workoutSessionRepository;

    @Mock
    ActiveRoutineRepository activeRoutineRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    WorkoutSessionCommandServiceImpl commandService;

    private void savesReturnTheSession() {
        when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static RecordSetCommand recordSquat(WorkoutSession session) {
        return new RecordSetCommand(session.getId(), CLIENT_ID, SQUAT.exerciseId(), 1,
                new Load(new BigDecimal("60")), new Reps(10));
    }

    @Test
    void shouldScheduleTheFirstDayWhenNothingWasFinishedYet() {
        // Arrange
        when(workoutSessionRepository.findByClientIdAndScheduledFor(CLIENT_ID, TODAY)).thenReturn(Optional.empty());
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeRoutine()));
        when(workoutSessionRepository.findLastFinishedByClientId(CLIENT_ID)).thenReturn(Optional.empty());
        savesReturnTheSession();

        // Act
        var session = commandService.handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY)).orElseThrow();

        // Assert
        assertThat(session.getDayOrder()).isEqualTo(LEGS_DAY.order());
        assertThat(session.getScheduledFor()).isEqualTo(TODAY);
        verify(eventPublisher).publishEvent(any(WorkoutSessionScheduled.class));
    }

    @Test
    void shouldScheduleTheDayThatFollowsTheLastFinishedOne() {
        when(workoutSessionRepository.findByClientIdAndScheduledFor(CLIENT_ID, TODAY)).thenReturn(Optional.empty());
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeRoutine()));
        when(workoutSessionRepository.findLastFinishedByClientId(CLIENT_ID))
                .thenReturn(Optional.of(pendingSession(TODAY.minusDays(1))));
        savesReturnTheSession();

        var session = commandService.handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY)).orElseThrow();

        assertThat(session.getDayOrder()).isEqualTo(BACK_DAY.order());
    }

    @Test
    void shouldStartANewRoutineFromItsFirstDay() {
        var finishedUnderPreviousRoutine = pendingSession(TODAY.minusDays(1));
        finishedUnderPreviousRoutine.setRoutineId(new RoutineId(UUID.randomUUID()));
        when(workoutSessionRepository.findByClientIdAndScheduledFor(CLIENT_ID, TODAY)).thenReturn(Optional.empty());
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeRoutine()));
        when(workoutSessionRepository.findLastFinishedByClientId(CLIENT_ID))
                .thenReturn(Optional.of(finishedUnderPreviousRoutine));
        savesReturnTheSession();

        var session = commandService.handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY)).orElseThrow();

        assertThat(session.getDayOrder()).isEqualTo(LEGS_DAY.order());
    }

    @Test
    void shouldReturnTheSessionAlreadyScheduledForThatDate() {
        var existing = pendingSession(TODAY);
        when(workoutSessionRepository.findByClientIdAndScheduledFor(CLIENT_ID, TODAY)).thenReturn(Optional.of(existing));

        var session = commandService.handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY));

        assertThat(session).containsSame(existing);
        verify(workoutSessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldNotScheduleWhenTheRoutineIsNoLongerActive() {
        var ended = activeRoutine();
        ended.end(new EndActiveRoutineCommand(CLIENT_ID, TODAY.minusDays(1)));
        when(workoutSessionRepository.findByClientIdAndScheduledFor(CLIENT_ID, TODAY)).thenReturn(Optional.empty());
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(ended));

        assertThatThrownBy(() -> commandService.handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY)))
                .isInstanceOf(ActiveRoutineNotFoundException.class);
    }

    @Test
    void shouldRecordASetAndPublishSetRecorded() {
        var session = pendingSession(TODAY);
        when(workoutSessionRepository.findByIdAndClientId(session.getId(), CLIENT_ID)).thenReturn(Optional.of(session));
        savesReturnTheSession();

        var saved = commandService.handle(recordSquat(session)).orElseThrow();

        assertThat(saved.getExercises().getFirst().getSets()).hasSize(1);
        var event = ArgumentCaptor.forClass(SetRecorded.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().exerciseId()).isEqualTo(SQUAT.exerciseId().value());
        assertThat(event.getValue().setNumber()).isEqualTo(1);
    }

    @Test
    void shouldAnswerNotFoundForAnotherClientsSession() {
        var session = pendingSession(TODAY);
        when(workoutSessionRepository.findByIdAndClientId(session.getId(), CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(recordSquat(session)))
                .isInstanceOf(WorkoutSessionNotFoundException.class);
        verify(workoutSessionRepository, never()).save(any());
    }

    @Test
    void shouldCorrectASetWithoutPublishingSetRecorded() {
        var session = pendingSession(TODAY);
        session.recordSet(recordSquat(session));
        when(workoutSessionRepository.findByIdAndClientId(session.getId(), CLIENT_ID)).thenReturn(Optional.of(session));
        savesReturnTheSession();

        var saved = commandService.handle(new CorrectSetCommand(session.getId(), CLIENT_ID, SQUAT.exerciseId(), 1,
                new Load(new BigDecimal("55")), new Reps(12))).orElseThrow();

        assertThat(saved.getExercises().getFirst().getSets().getFirst().getLoad().kilograms())
                .isEqualByComparingTo("55");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldFinishASessionAndPublishItsStatus() {
        var session = pendingSession(TODAY);
        session.recordSet(recordSquat(session));
        when(workoutSessionRepository.findByIdAndClientId(session.getId(), CLIENT_ID)).thenReturn(Optional.of(session));
        savesReturnTheSession();

        var saved = commandService.handle(new FinishWorkoutSessionCommand(session.getId(), CLIENT_ID, true))
                .orElseThrow();

        assertThat(saved.getStatus()).isEqualTo(ComplianceStatus.PARTIAL);
        var event = ArgumentCaptor.forClass(WorkoutSessionFinished.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().status()).isEqualTo("PARTIAL");
    }

    @Test
    void shouldCloseOverdueSessionsAsSkippedOrPartial() {
        var untouched = pendingSession(TODAY.minusDays(1));
        var started = pendingSession(TODAY.minusDays(2));
        started.recordSet(recordSquat(started));
        when(workoutSessionRepository.findAllPendingBefore(TODAY)).thenReturn(List.of(untouched, started));
        savesReturnTheSession();

        commandService.handle(new CloseOverdueWorkoutSessionsCommand(TODAY));

        assertThat(untouched.getStatus()).isEqualTo(ComplianceStatus.SKIPPED);
        assertThat(started.getStatus()).isEqualTo(ComplianceStatus.PARTIAL);
        assertThat(started.getFinishedAt()).isNotNull();
        verify(workoutSessionRepository).save(untouched);
        verify(workoutSessionRepository).save(started);
        verify(eventPublisher).publishEvent(new WorkoutSessionSkipped(untouched.getId().value(), CLIENT_ID.value()));
        verify(eventPublisher).publishEvent(new WorkoutSessionFinished(started.getId().value(), CLIENT_ID.value(),
                "PARTIAL"));
    }

    @Test
    void shouldLookUpTheSessionByIdAndOwner() {
        var id = new WorkoutSessionId(UUID.randomUUID());
        when(workoutSessionRepository.findByIdAndClientId(id, CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(new FinishWorkoutSessionCommand(id, CLIENT_ID, false)))
                .isInstanceOf(WorkoutSessionNotFoundException.class);
    }

    @Test
    void shouldNotScheduleASessionOnARestDay() {
        var routine = activeRoutine();
        routine.setTrainingDays(EnumSet.of(TODAY.plusDays(1).getDayOfWeek()));
        when(workoutSessionRepository.findByClientIdAndScheduledFor(CLIENT_ID, TODAY)).thenReturn(Optional.empty());
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(routine));

        assertThat(commandService.handle(new ScheduleWorkoutSessionCommand(CLIENT_ID, TODAY))).isEmpty();
        verify(workoutSessionRepository, never()).save(any(WorkoutSession.class));
    }
}
