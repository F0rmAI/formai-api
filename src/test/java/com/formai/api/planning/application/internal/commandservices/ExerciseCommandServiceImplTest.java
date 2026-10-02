package com.formai.api.planning.application.internal.commandservices;

import com.formai.api.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import com.formai.api.planning.domain.exceptions.ExerciseAlreadyExistsException;
import com.formai.api.planning.domain.exceptions.ExerciseInUseException;
import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.exceptions.MachineNotPublishedException;
import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.commands.ArchiveExerciseCommand;
import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.commands.DeleteExerciseCommand;
import com.formai.api.planning.domain.model.commands.LinkExerciseToMachineCommand;
import com.formai.api.planning.domain.model.commands.RestoreExerciseCommand;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.MachineId;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.domain.repositories.ExerciseRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.squat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseCommandServiceImplTest {

    private static final CreateExerciseCommand CREATE_SQUAT = new CreateExerciseCommand(TRAINER_HOLDER_ID,
            new ExerciseName("Squat"), new MuscleGroup("Legs"), Optional.empty());

    @Mock
    ExerciseRepository exerciseRepository;

    @Mock
    RoutineRepository routineRepository;

    @Mock
    ExternalCatalogService externalCatalogService;

    @InjectMocks
    ExerciseCommandServiceImpl commandService;

    private void savesReturnTheExercise() {
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void shouldAddTheExerciseToTheCatalog() {
        // Arrange
        when(exerciseRepository.existsByHolderIdAndName(TRAINER_HOLDER_ID, CREATE_SQUAT.name())).thenReturn(false);
        savesReturnTheExercise();

        // Act
        var exercise = commandService.handle(CREATE_SQUAT).orElseThrow();

        // Assert
        assertThat(exercise.getName().value()).isEqualTo("Squat");
        assertThat(exercise.getStatus()).isEqualTo(ExerciseStatus.ACTIVE);
    }

    @Test
    void shouldRejectADuplicateName() {
        when(exerciseRepository.existsByHolderIdAndName(TRAINER_HOLDER_ID, CREATE_SQUAT.name())).thenReturn(true);

        assertThatThrownBy(() -> commandService.handle(CREATE_SQUAT))
                .isInstanceOf(ExerciseAlreadyExistsException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void shouldArchiveAndRestoreAnExercise() {
        var exercise = squat();
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(exercise));
        savesReturnTheExercise();

        var archived = commandService.handle(new ArchiveExerciseCommand(SQUAT_ID, TRAINER_HOLDER_ID)).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(ExerciseStatus.ARCHIVED);

        var restored = commandService.handle(new RestoreExerciseCommand(SQUAT_ID, TRAINER_HOLDER_ID)).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(ExerciseStatus.ACTIVE);
    }

    @Test
    void shouldAnswerNotFoundForAnotherTrainersExercise() {
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(new ArchiveExerciseCommand(SQUAT_ID, TRAINER_HOLDER_ID)))
                .isInstanceOf(ExerciseNotFoundException.class);
    }

    @Test
    void shouldDeleteAnExerciseNoRoutineUses() {
        var exercise = squat();
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(exercise));
        when(routineRepository.existsByHolderIdAndExerciseId(TRAINER_HOLDER_ID, SQUAT_ID)).thenReturn(false);

        commandService.handle(new DeleteExerciseCommand(SQUAT_ID, TRAINER_HOLDER_ID));

        verify(exerciseRepository).delete(exercise);
    }

    @Test
    void shouldOnlyAllowArchivingAnExerciseUsedInARoutine() {
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(squat()));
        when(routineRepository.existsByHolderIdAndExerciseId(TRAINER_HOLDER_ID, SQUAT_ID)).thenReturn(true);

        assertThatThrownBy(() -> commandService.handle(new DeleteExerciseCommand(SQUAT_ID, TRAINER_HOLDER_ID)))
                .isInstanceOf(ExerciseInUseException.class);
        verify(exerciseRepository, never()).delete(any());
    }

    @Test
    void shouldRejectLinkingAMachineThatIsNotPublished() {
        // Arrange
        var machineId = new MachineId(UUID.randomUUID());
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(squat()));
        when(externalCatalogService.isMachinePublished(machineId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> commandService.handle(
                new LinkExerciseToMachineCommand(SQUAT_ID, TRAINER_HOLDER_ID, machineId)))
                .isInstanceOf(MachineNotPublishedException.class);
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    void shouldLinkAPublishedMachineToTheExercise() {
        // Arrange
        var machineId = new MachineId(UUID.randomUUID());
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(squat()));
        when(externalCatalogService.isMachinePublished(machineId)).thenReturn(true);
        savesReturnTheExercise();

        // Act
        var exercise = commandService.handle(
                new LinkExerciseToMachineCommand(SQUAT_ID, TRAINER_HOLDER_ID, machineId)).orElseThrow();

        // Assert
        assertThat(exercise.getMachineId()).isEqualTo(machineId);
    }

    @Test
    void shouldAnswerNotFoundWhenLinkingAnotherTrainersExercise() {
        var machineId = new MachineId(UUID.randomUUID());
        when(exerciseRepository.findByIdAndHolderId(SQUAT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(
                new LinkExerciseToMachineCommand(SQUAT_ID, TRAINER_HOLDER_ID, machineId)))
                .isInstanceOf(ExerciseNotFoundException.class);
        verify(externalCatalogService, never()).isMachinePublished(any());
    }
}
