package com.formai.api.planning.application.internal.commandservices;

import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.exceptions.InvalidRoutineException;
import com.formai.api.planning.domain.exceptions.RoutineNotFoundException;
import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.commands.CloseRoutineCommand;
import com.formai.api.planning.domain.model.commands.CreateRoutineCommand;
import com.formai.api.planning.domain.model.commands.DuplicateRoutineCommand;
import com.formai.api.planning.domain.model.commands.MarkRoutineActiveCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;
import com.formai.api.planning.domain.model.entities.PrescribedExercise;
import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.events.RoutineUpdated;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;
import com.formai.api.planning.domain.model.valueobjects.RoutineStatus;
import com.formai.api.planning.domain.repositories.ClientPlanRepository;
import com.formai.api.planning.domain.repositories.ExerciseRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.prescription;
import static com.formai.api.planning.PlanningTestData.routine;
import static com.formai.api.planning.PlanningTestData.squat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutineCommandServiceImplTest {

    @Mock
    RoutineRepository routineRepository;

    @Mock
    ExerciseRepository exerciseRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    ClientPlanRepository clientPlanRepository;

    @InjectMocks
    RoutineCommandServiceImpl commandService;

    private static List<RoutineSession> legsDayWithoutNames() {
        return List.of(new RoutineSession(1, "Day A · Legs",
                List.of(new PrescribedExercise(SQUAT_ID, null, prescription(3, 10, "60")))));
    }

    private void savesReturnTheRoutine() {
        when(routineRepository.save(any(Routine.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void shouldCreateTheRoutineCopyingNamesFromTheCatalog() {
        // Arrange
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(squat()));
        savesReturnTheRoutine();

        // Act
        var routine = commandService.handle(new CreateRoutineCommand(TRAINER_HOLDER_ID,
                new RoutineName("Legs only"), legsDayWithoutNames())).orElseThrow();

        // Assert
        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.DRAFT);
        assertThat(routine.currentVersion().getSessions().getFirst().getExercises().getFirst().getExerciseName())
                .isEqualTo("Squat");
    }

    @Test
    void shouldRejectAnExerciseThatIsNotInTheCatalog() {
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(new CreateRoutineCommand(TRAINER_HOLDER_ID,
                new RoutineName("Legs only"), legsDayWithoutNames())))
                .isInstanceOf(ExerciseNotFoundException.class);
        verify(routineRepository, never()).save(any());
    }

    @Test
    void shouldRejectAnArchivedExercise() {
        var archived = squat();
        archived.archive();
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> commandService.handle(new CreateRoutineCommand(TRAINER_HOLDER_ID,
                new RoutineName("Legs only"), legsDayWithoutNames())))
                .isInstanceOf(InvalidRoutineException.class);
    }

    @Test
    void shouldSaveAChangeAsANewVersionAndPublishRoutineUpdated() {
        var routine = routine();
        when(routineRepository.findByIdAndHolderId(routine.getId(), TRAINER_HOLDER_ID)).thenReturn(Optional.of(routine));
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(squat()));
        savesReturnTheRoutine();

        var updated = commandService.handle(new UpdateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID,
                new RoutineName("Legs only v2"), legsDayWithoutNames())).orElseThrow();

        assertThat(updated.getVersions()).hasSize(2);
        verify(eventPublisher).publishEvent(new RoutineUpdated(routine.getId().value(), 2));
    }

    @Test
    void shouldAnswerNotFoundWhenModifyingAnotherTrainersRoutine() {
        var routine = routine();
        when(routineRepository.findByIdAndHolderId(routine.getId(), TRAINER_HOLDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(new UpdateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID,
                routine.getName(), legsDayWithoutNames())))
                .isInstanceOf(RoutineNotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldDuplicateARoutineAsANewDraft() {
        var routine = routine();
        when(routineRepository.findByIdAndHolderId(routine.getId(), TRAINER_HOLDER_ID)).thenReturn(Optional.of(routine));
        savesReturnTheRoutine();

        var copy = commandService.handle(new DuplicateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID,
                new RoutineName("Copy"))).orElseThrow();

        assertThat(copy.getId()).isNotEqualTo(routine.getId());
        assertThat(copy.getName().value()).isEqualTo("Copy");
    }

    @Test
    void shouldMarkTheRoutineActive() {
        var routine = routine();
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        commandService.handle(new MarkRoutineActiveCommand(routine.getId()));

        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.ACTIVE);
        verify(routineRepository).save(routine);
    }

    @Test
    void shouldCloseTheRoutineWhenNoClientHasItOpen() {
        var routine = routine();
        routine.markActive();
        when(clientPlanRepository.existsOpenAssignmentByRoutineId(routine.getId())).thenReturn(false);
        when(routineRepository.findById(routine.getId())).thenReturn(Optional.of(routine));

        commandService.handle(new CloseRoutineCommand(routine.getId()));

        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.CLOSED);
        verify(routineRepository).save(routine);
    }

    @Test
    void shouldKeepTheRoutineActiveWhileAClientStillHasIt() {
        var routine = routine();
        when(clientPlanRepository.existsOpenAssignmentByRoutineId(routine.getId())).thenReturn(true);

        commandService.handle(new CloseRoutineCommand(routine.getId()));

        verify(routineRepository, never()).findById(any());
        verify(routineRepository, never()).save(any());
    }
}
