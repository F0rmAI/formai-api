package com.formai.api.planning.domain.model.aggregates;

import com.formai.api.planning.domain.exceptions.InvalidRoutineException;
import com.formai.api.planning.domain.model.commands.CreateRoutineCommand;
import com.formai.api.planning.domain.model.commands.DuplicateRoutineCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;
import com.formai.api.planning.domain.model.entities.PrescribedExercise;
import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.Prescription;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;
import com.formai.api.planning.domain.model.valueobjects.RoutineStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.prescription;
import static com.formai.api.planning.PlanningTestData.routine;
import static com.formai.api.planning.PlanningTestData.twoSessions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutineTest {

    private static List<RoutineSession> heavierSquat() {
        return List.of(new RoutineSession(1, "Day A · Legs",
                List.of(new PrescribedExercise(SQUAT_ID, "Squat", prescription(4, 8, "70")))));
    }

    @Test
    void shouldCreateADraftAsVersionOne() {
        // Act
        var routine = routine();

        // Assert
        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.DRAFT);
        assertThat(routine.getVersions()).hasSize(1);
        assertThat(routine.currentVersion().getNumber()).isEqualTo(1);
        assertThat(routine.currentVersion().getAuthor()).isEqualTo(TRAINER_HOLDER_ID);
        assertThat(routine.currentVersion().getSessions()).hasSize(2);
    }

    @Test
    void shouldRejectARoutineWithoutSessions() {
        var command = new CreateRoutineCommand(TRAINER_HOLDER_ID, new RoutineName("Empty"), List.of());

        assertThatThrownBy(() -> Routine.create(command)).isInstanceOf(InvalidRoutineException.class);
    }

    @Test
    void shouldRejectASessionWithoutExercises() {
        var command = new CreateRoutineCommand(TRAINER_HOLDER_ID, new RoutineName("Empty day"),
                List.of(new RoutineSession(1, "Day A", List.of())));

        assertThatThrownBy(() -> Routine.create(command)).isInstanceOf(InvalidRoutineException.class);
    }

    @Test
    void shouldRejectSetsOrRepsOfZeroOrLessAndNegativeLoadOrRest() {
        assertThatThrownBy(() -> new Prescription(0, 10, BigDecimal.TEN, 60)).isInstanceOf(InvalidRoutineException.class);
        assertThatThrownBy(() -> new Prescription(3, -1, BigDecimal.TEN, 60)).isInstanceOf(InvalidRoutineException.class);
        assertThatThrownBy(() -> new Prescription(3, 10, new BigDecimal("-1"), 60))
                .isInstanceOf(InvalidRoutineException.class);
        assertThatThrownBy(() -> new Prescription(3, 10, BigDecimal.TEN, -5)).isInstanceOf(InvalidRoutineException.class);
    }

    @Test
    void shouldAddAVersionOnEveryChangeAndKeepThePreviousOnes() {
        var routine = routine();

        var version = routine.revise(new UpdateRoutineCommand(routine.getId(), "another-author",
                new RoutineName("Strength v2"), heavierSquat()));

        assertThat(version.getNumber()).isEqualTo(2);
        assertThat(version.getAuthor()).isEqualTo("another-author");
        assertThat(routine.getName().value()).isEqualTo("Strength v2");
        assertThat(routine.getVersions()).hasSize(2);
        assertThat(routine.currentVersion()).isSameAs(version);
        assertThat(routine.getVersions().getFirst().getSessions()).hasSize(2);
    }

    @Test
    void shouldDuplicateTheCurrentVersionAsANewDraft() {
        var routine = routine();
        routine.revise(new UpdateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID, routine.getName(), heavierSquat()));
        routine.markActive();

        var copy = routine.duplicate(new DuplicateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID,
                new RoutineName("Strength copy")));

        assertThat(copy.getId()).isNotEqualTo(routine.getId());
        assertThat(copy.getStatus()).isEqualTo(RoutineStatus.DRAFT);
        assertThat(copy.getVersions()).hasSize(1);
        assertThat(copy.currentVersion().getSessions().getFirst().getExercises().getFirst().getPrescription().sets())
                .isEqualTo(4);
    }

    @Test
    void shouldKnowWhichExercisesItUses() {
        var routine = routine();

        assertThat(routine.usesExercise(SQUAT_ID)).isTrue();
        assertThat(routine.usesExercise(new ExerciseId(UUID.randomUUID()))).isFalse();
    }

    @Test
    void shouldBecomeActiveWhenMarked() {
        var routine = routine();

        routine.markActive();
        routine.markActive();

        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.ACTIVE);
    }

    @Test
    void shouldNotReviseWithAnInvalidSessionList() {
        var routine = routine();

        assertThatThrownBy(() -> routine.revise(new UpdateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID,
                routine.getName(), List.of())))
                .isInstanceOf(InvalidRoutineException.class);
        assertThat(routine.getVersions()).hasSize(1);
    }

    @Test
    void shouldCloseAnActiveRoutineAndReopenItWhenAssignedAgain() {
        var routine = routine();
        routine.markActive();

        routine.close();
        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.CLOSED);

        routine.markActive();
        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.ACTIVE);
    }

    @Test
    void shouldKeepADraftAsADraftWhenClosed() {
        var routine = routine();

        routine.close();

        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.DRAFT);
    }

    @Test
    void shouldAcceptARevisionWhileClosed() {
        var routine = routine();
        routine.markActive();
        routine.close();

        routine.revise(new UpdateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID, routine.getName(), twoSessions()));

        assertThat(routine.currentVersion().getNumber()).isEqualTo(2);
        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.CLOSED);
    }
}
