package com.formai.api.planning.domain.model.aggregates;

import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.squat;
import static org.assertj.core.api.Assertions.assertThat;

class ExerciseTest {

    @Test
    void shouldCreateAnActiveExerciseOfTheTrainer() {
        // Act
        var exercise = squat();

        // Assert
        assertThat(exercise.getHolderId()).isEqualTo(TRAINER_HOLDER_ID);
        assertThat(exercise.getName().value()).isEqualTo("Squat");
        assertThat(exercise.getMuscleGroup().value()).isEqualTo("Legs");
        assertThat(exercise.getEquipment()).isEqualTo("Barbell");
        assertThat(exercise.getStatus()).isEqualTo(ExerciseStatus.ACTIVE);
    }

    @Test
    void shouldLeaveEquipmentEmptyWhenNoneIsGiven() {
        var exercise = Exercise.create(new CreateExerciseCommand(TRAINER_HOLDER_ID, new ExerciseName(" Push-up "),
                new MuscleGroup("Chest"), Optional.of("  ")));

        assertThat(exercise.getName().value()).isEqualTo("Push-up");
        assertThat(exercise.getEquipment()).isNull();
    }

    @Test
    void shouldArchiveAndRestore() {
        var exercise = squat();

        exercise.archive();
        assertThat(exercise.isActive()).isFalse();

        exercise.restore();
        exercise.restore();
        assertThat(exercise.getStatus()).isEqualTo(ExerciseStatus.ACTIVE);
    }
}
