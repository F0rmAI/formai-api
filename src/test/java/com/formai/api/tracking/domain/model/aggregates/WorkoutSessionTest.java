package com.formai.api.tracking.domain.model.aggregates;

import com.formai.api.tracking.domain.exceptions.InvalidSetValueException;
import com.formai.api.tracking.domain.exceptions.PartialFinishNotConfirmedException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionAlreadyFinishedException;
import com.formai.api.tracking.domain.model.commands.CorrectSetCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.valueobjects.ComplianceStatus;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseToPerform;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.formai.api.tracking.TrackingTestData.BENCH_PRESS;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.ROUTINE_ID;
import static com.formai.api.tracking.TrackingTestData.SQUAT;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.pendingSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkoutSessionTest {

    private static RecordSetCommand recordSet(WorkoutSession session, ExerciseToPerform exercise, int setNumber,
                                              String loadKg, int reps) {
        return new RecordSetCommand(session.getId(), CLIENT_ID, exercise.exerciseId(), setNumber,
                new Load(new BigDecimal(loadKg)), new Reps(reps));
    }

    private static CorrectSetCommand correctSet(WorkoutSession session, ExerciseToPerform exercise, int setNumber,
                                                String loadKg, int reps) {
        return new CorrectSetCommand(session.getId(), CLIENT_ID, exercise.exerciseId(), setNumber,
                new Load(new BigDecimal(loadKg)), new Reps(reps));
    }

    private static FinishWorkoutSessionCommand finish(WorkoutSession session, boolean confirmPartial) {
        return new FinishWorkoutSessionCommand(session.getId(), CLIENT_ID, confirmPartial);
    }

    private static void recordEveryExercise(WorkoutSession session) {
        session.recordSet(recordSet(session, SQUAT, 1, "60", 10));
        session.recordSet(recordSet(session, BENCH_PRESS, 1, "40", 8));
    }

    @Test
    void shouldScheduleAPendingSessionWithTheDayPrescription() {
        // Act
        var session = pendingSession(TODAY);

        // Assert
        assertThat(session.getStatus()).isEqualTo(ComplianceStatus.PENDING);
        assertThat(session.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(session.getRoutineId()).isEqualTo(ROUTINE_ID);
        assertThat(session.getRoutineVersion()).isEqualTo(1);
        assertThat(session.getDayOrder()).isEqualTo(1);
        assertThat(session.getScheduledFor()).isEqualTo(TODAY);
        assertThat(session.getExercises()).extracting(exercise -> exercise.getToPerform())
                .containsExactly(SQUAT, BENCH_PRESS);
        assertThat(session.hasRecords()).isFalse();
    }

    @Test
    void shouldRecordLoadAndRepsAndAddThemToTheVolume() {
        var session = pendingSession(TODAY);

        session.recordSet(recordSet(session, SQUAT, 1, "60", 10));
        session.recordSet(recordSet(session, SQUAT, 2, "62.5", 8));

        var sets = session.getExercises().getFirst().getSets();
        assertThat(sets).hasSize(2);
        assertThat(sets.get(1).getLoad().kilograms()).isEqualByComparingTo("62.5");
        assertThat(sets.get(1).getReps().value()).isEqualTo(8);
        assertThat(sets.get(1).getRecordedAt()).isNotNull();
        assertThat(session.volume().kilograms()).isEqualByComparingTo("1100");   // 60×10 + 62.5×8
    }

    @Test
    void shouldReplaceASetRecordedTwiceInsteadOfDuplicatingIt() {
        var session = pendingSession(TODAY);

        session.recordSet(recordSet(session, SQUAT, 1, "60", 10));
        session.recordSet(recordSet(session, SQUAT, 1, "65", 9));

        var sets = session.getExercises().getFirst().getSets();
        assertThat(sets).hasSize(1);
        assertThat(sets.getFirst().getLoad().kilograms()).isEqualByComparingTo("65");
    }

    @Test
    void shouldCorrectARecordedSetKeepingItsRecordingTime() {
        var session = pendingSession(TODAY);
        session.recordSet(recordSet(session, SQUAT, 1, "60", 10));
        var recordedAt = session.getExercises().getFirst().getSets().getFirst().getRecordedAt();

        session.correctSet(correctSet(session, SQUAT, 1, "55", 12));

        var sets = session.getExercises().getFirst().getSets();
        assertThat(sets).hasSize(1);
        assertThat(sets.getFirst().getLoad().kilograms()).isEqualByComparingTo("55");
        assertThat(sets.getFirst().getReps().value()).isEqualTo(12);
        assertThat(sets.getFirst().getRecordedAt()).isEqualTo(recordedAt);
    }

    @Test
    void shouldRejectCorrectingASetNotRecordedYet() {
        var session = pendingSession(TODAY);

        assertThatThrownBy(() -> session.correctSet(correctSet(session, SQUAT, 1, "55", 12)))
                .isInstanceOf(InvalidSetValueException.class);
    }

    @Test
    void shouldRejectASetNumberOutsideThePrescription() {
        var session = pendingSession(TODAY);

        assertThatThrownBy(() -> session.recordSet(recordSet(session, SQUAT, 4, "60", 10)))
                .isInstanceOf(InvalidSetValueException.class);
        assertThatThrownBy(() -> session.recordSet(recordSet(session, SQUAT, 0, "60", 10)))
                .isInstanceOf(InvalidSetValueException.class);
    }

    @Test
    void shouldRejectAnExerciseThatIsNotInTheSession() {
        var session = pendingSession(TODAY);
        var command = new RecordSetCommand(session.getId(), CLIENT_ID, new ExerciseId(UUID.randomUUID()), 1,
                new Load(BigDecimal.TEN), new Reps(10));

        assertThatThrownBy(() -> session.recordSet(command)).isInstanceOf(InvalidSetValueException.class);
    }

    @Test
    void shouldRejectNegativeLoadOrReps() {
        assertThatThrownBy(() -> new Load(new BigDecimal("-1"))).isInstanceOf(InvalidSetValueException.class);
        assertThatThrownBy(() -> new Reps(-1)).isInstanceOf(InvalidSetValueException.class);
    }

    @Test
    void shouldAcceptZeroLoadAndZeroReps() {
        var session = pendingSession(TODAY);

        session.recordSet(recordSet(session, SQUAT, 1, "0", 0));

        assertThat(session.hasRecords()).isTrue();
    }

    @Test
    void shouldFinishAsCompletedWhenEveryExerciseHasSets() {
        var session = pendingSession(TODAY);
        recordEveryExercise(session);

        var status = session.finish(finish(session, false));

        assertThat(status).isEqualTo(ComplianceStatus.COMPLETED);
        assertThat(session.getStatus()).isEqualTo(ComplianceStatus.COMPLETED);
        assertThat(session.getFinishedAt()).isNotNull();
    }

    @Test
    void shouldRequireConfirmationToFinishWithExercisesLeftWithoutSets() {
        var session = pendingSession(TODAY);
        session.recordSet(recordSet(session, SQUAT, 1, "60", 10));

        assertThatThrownBy(() -> session.finish(finish(session, false)))
                .isInstanceOf(PartialFinishNotConfirmedException.class);
        assertThat(session.getStatus()).isEqualTo(ComplianceStatus.PENDING);
        assertThat(session.getFinishedAt()).isNull();
    }

    @Test
    void shouldFinishAsPartialOnceTheClientConfirms() {
        var session = pendingSession(TODAY);
        session.recordSet(recordSet(session, SQUAT, 1, "60", 10));

        var status = session.finish(finish(session, true));

        assertThat(status).isEqualTo(ComplianceStatus.PARTIAL);
        assertThat(session.getFinishedAt()).isNotNull();
    }

    @Test
    void shouldRejectAnyChangeOnceFinished() {
        var session = pendingSession(TODAY);
        recordEveryExercise(session);
        session.finish(finish(session, false));

        assertThatThrownBy(() -> session.recordSet(recordSet(session, SQUAT, 2, "60", 10)))
                .isInstanceOf(WorkoutSessionAlreadyFinishedException.class);
        assertThatThrownBy(() -> session.correctSet(correctSet(session, SQUAT, 1, "60", 10)))
                .isInstanceOf(WorkoutSessionAlreadyFinishedException.class);
        assertThatThrownBy(() -> session.finish(finish(session, true)))
                .isInstanceOf(WorkoutSessionAlreadyFinishedException.class);
    }

    @Test
    void shouldSkipAPendingSession() {
        var session = pendingSession(TODAY);

        session.skip();

        assertThat(session.getStatus()).isEqualTo(ComplianceStatus.SKIPPED);
        assertThatThrownBy(() -> session.recordSet(recordSet(session, SQUAT, 1, "60", 10)))
                .isInstanceOf(WorkoutSessionAlreadyFinishedException.class);
    }
}
