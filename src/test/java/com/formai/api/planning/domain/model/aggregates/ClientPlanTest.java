package com.formai.api.planning.domain.model.aggregates;

import com.formai.api.planning.domain.exceptions.AssignmentNotFoundException;
import com.formai.api.planning.domain.exceptions.InvalidAdjustmentException;
import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.PersonalizeAssignmentCommand;
import com.formai.api.planning.domain.model.commands.TransferClientPlanCommand;
import com.formai.api.planning.domain.model.valueobjects.AssignmentNote;
import com.formai.api.planning.domain.model.valueobjects.PrescriptionAdjustment;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.DEADLIFT_ID;
import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.START_DATE;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.emptyPlan;
import static com.formai.api.planning.PlanningTestData.routine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientPlanTest {

    private static final RoutineId FIRST_ROUTINE = new RoutineId(UUID.randomUUID());
    private static final RoutineId SECOND_ROUTINE = new RoutineId(UUID.randomUUID());

    private static AssignRoutineCommand assign(RoutineId routineId, LocalDate startDate) {
        return new AssignRoutineCommand(routineId, CLIENT_ID, TRAINER_HOLDER_ID, startDate, TrainingDays.everyDay());
    }

    @Test
    void shouldStartWithoutACurrentAssignment() {
        assertThat(emptyPlan().currentAssignment()).isEmpty();
    }

    @Test
    void shouldMakeTheNewAssignmentCurrentFromItsStartDate() {
        // Arrange
        var plan = emptyPlan();

        // Act
        var assignment = plan.assign(assign(FIRST_ROUTINE, START_DATE));

        // Assert
        assertThat(plan.currentAssignment()).containsSame(assignment);
        assertThat(assignment.getRoutineId()).isEqualTo(FIRST_ROUTINE);
        assertThat(assignment.getPeriod().startDate()).isEqualTo(START_DATE);
        assertThat(assignment.getPeriod().endDate()).isNull();
    }

    @Test
    void shouldCloseThePreviousAssignmentTheDayBeforeTheNewOneAndKeepIt() {
        var plan = emptyPlan();
        var first = plan.assign(assign(FIRST_ROUTINE, START_DATE));

        var second = plan.assign(assign(SECOND_ROUTINE, START_DATE.plusDays(30)));

        assertThat(first.isCurrent()).isFalse();
        assertThat(first.getPeriod().endDate()).isEqualTo(START_DATE.plusDays(29));
        assertThat(plan.currentAssignment()).containsSame(second);
        assertThat(plan.getAssignments()).containsExactly(first, second);
    }

    @Test
    void shouldNeverEndThePreviousAssignmentBeforeItStarted() {
        var plan = emptyPlan();
        var first = plan.assign(assign(FIRST_ROUTINE, START_DATE));

        plan.assign(assign(SECOND_ROUTINE, START_DATE));

        assertThat(first.getPeriod().endDate()).isEqualTo(START_DATE);
    }

    @Test
    void shouldCloseTheCurrentAssignment() {
        var plan = emptyPlan();
        var assignment = plan.assign(assign(FIRST_ROUTINE, START_DATE));

        plan.closeCurrentAssignment(START_DATE.plusDays(10));

        assertThat(assignment.getPeriod().endDate()).isEqualTo(START_DATE.plusDays(10));
        assertThat(plan.currentAssignment()).isEmpty();
    }

    @Test
    void shouldKeepTheTrainingDaysOfTheAssignment() {
        var plan = emptyPlan();
        var mondayAndThursday = TrainingDays.ofNames(List.of("MONDAY", "THURSDAY"));

        var assignment = plan.assign(new AssignRoutineCommand(new RoutineId(UUID.randomUUID()), CLIENT_ID,
                TRAINER_HOLDER_ID, START_DATE, mondayAndThursday));

        assertThat(assignment.getTrainingDays().names()).containsExactly("MONDAY", "THURSDAY");
    }

    @Test
    void shouldCloseTheCurrentAssignmentAndFollowTheNewTrainerWhenTransferred() {
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(new RoutineId(UUID.randomUUID()), CLIENT_ID, TRAINER_HOLDER_ID,
                START_DATE, TrainingDays.everyDay()));
        var newTrainer = "33333333-3333-3333-3333-333333333333";

        plan.transferTo(new TransferClientPlanCommand(CLIENT_ID, newTrainer, START_DATE.plusDays(10)));

        assertThat(plan.getHolderId()).isEqualTo(newTrainer);
        assertThat(plan.currentAssignment()).isEmpty();
        assertThat(plan.getAssignments().getFirst().getPeriod().endDate()).isEqualTo(START_DATE.plusDays(9));
    }

    @Test
    void shouldKeepTheNoteGivenWhenAssigning() {
        var plan = emptyPlan();

        var assignment = plan.assign(new AssignRoutineCommand(FIRST_ROUTINE, CLIENT_ID, TRAINER_HOLDER_ID, START_DATE,
                TrainingDays.everyDay(), Optional.of(new AssignmentNote("  Start light  "))));

        assertThat(assignment.getNote().value()).isEqualTo("Start light");
        assertThat(assignment.getAdjustments()).isEmpty();
    }

    @Test
    void shouldPersonalizeTheCurrentAssignmentWithoutTouchingTheRoutine() {
        var routine = routine();
        var plan = emptyPlan();
        plan.assign(assign(routine.getId(), START_DATE));

        var assignment = plan.personalizeCurrentAssignment(personalize(Optional.of(new AssignmentNote("Rest more")),
                List.of(new PrescriptionAdjustment(1, SQUAT_ID, new BigDecimal("40"), null))), routine.currentVersion());

        assertThat(assignment.getNote().value()).isEqualTo("Rest more");
        assertThat(assignment.getAdjustments()).hasSize(1);
        assertThat(routine.currentVersion().getSessions().getFirst().getExercises().getFirst().getPrescription()
                .targetLoadKg()).isEqualByComparingTo("60");
    }

    @Test
    void shouldRejectAnAdjustmentForAnExerciseTheSessionDoesNotPrescribe() {
        var routine = routine();
        var plan = emptyPlan();
        plan.assign(assign(routine.getId(), START_DATE));

        // Deadlift is in session 2, not in session 1.
        assertThatThrownBy(() -> plan.personalizeCurrentAssignment(personalize(Optional.empty(),
                List.of(new PrescriptionAdjustment(1, DEADLIFT_ID, BigDecimal.TEN, null))), routine.currentVersion()))
                .isInstanceOf(InvalidAdjustmentException.class);
    }

    @Test
    void shouldRejectTheSameExerciseOfASessionAdjustedTwice() {
        var routine = routine();
        var plan = emptyPlan();
        plan.assign(assign(routine.getId(), START_DATE));
        var adjustment = new PrescriptionAdjustment(1, SQUAT_ID, BigDecimal.TEN, null);

        assertThatThrownBy(() -> plan.personalizeCurrentAssignment(personalize(Optional.empty(),
                List.of(adjustment, adjustment)), routine.currentVersion()))
                .isInstanceOf(InvalidAdjustmentException.class);
    }

    @Test
    void shouldNotPersonalizeWithoutACurrentAssignment() {
        assertThatThrownBy(() -> emptyPlan().personalizeCurrentAssignment(personalize(Optional.empty(), List.of()),
                routine().currentVersion()))
                .isInstanceOf(AssignmentNotFoundException.class);
    }

    private static PersonalizeAssignmentCommand personalize(Optional<AssignmentNote> note,
                                                            List<PrescriptionAdjustment> adjustments) {
        return new PersonalizeAssignmentCommand(CLIENT_ID, TRAINER_HOLDER_ID, note, adjustments);
    }
}
