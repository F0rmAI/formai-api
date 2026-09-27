package com.formai.api.tracking.domain.services;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseToPerform;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.ProgressWindow;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.formai.api.tracking.TrackingTestData.BENCH_PRESS;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.SQUAT;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.pendingSession;
import static org.assertj.core.api.Assertions.assertThat;

class ProgressCalculatorTest {

    private final ProgressCalculator calculator = new ProgressCalculator();
    private final ReportPeriod period = new ReportPeriod(TODAY.minusDays(27), TODAY);

    private static void record(WorkoutSession session, ExerciseToPerform exercise, int setNumber, String loadKg, int reps) {
        session.recordSet(new RecordSetCommand(session.getId(), CLIENT_ID, exercise.exerciseId(), setNumber,
                new Load(new BigDecimal(loadKg)), new Reps(reps)));
    }

    private static List<WorkoutSession> threeSessions() {
        var partial = pendingSession(TODAY.minusDays(14));
        record(partial, SQUAT, 1, "60", 10);
        record(partial, SQUAT, 2, "65", 8);
        partial.finish(new FinishWorkoutSessionCommand(partial.getId(), CLIENT_ID, true));

        var completed = pendingSession(TODAY.minusDays(7));
        record(completed, SQUAT, 1, "70", 8);
        record(completed, BENCH_PRESS, 1, "40", 8);
        completed.finish(new FinishWorkoutSessionCommand(completed.getId(), CLIENT_ID, false));

        var skipped = pendingSession(TODAY.minusDays(3));
        skipped.skip();
        return List.of(skipped, completed, partial);
    }

    @Test
    void shouldReportAdherenceAndSessionsByStatus() {
        // Act
        var report = calculator.report(CLIENT_ID, period, threeSessions());

        // Assert
        assertThat(report.scheduled()).isEqualTo(3);
        assertThat(report.completed()).isEqualTo(1);
        assertThat(report.partial()).isEqualTo(1);
        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.adherence().percentage()).isEqualByComparingTo("33.33");
        assertThat(report.hasData()).isTrue();
    }

    @Test
    void shouldCompareEachExercisesFirstAndLastSession() {
        var metrics = calculator.exerciseMetrics(threeSessions());

        assertThat(metrics).extracting(metric -> metric.exerciseName()).containsExactly("Bench press", "Squat");
        var squat = metrics.getLast();
        assertThat(squat.firstMaxLoad().kilograms()).isEqualByComparingTo("65");
        assertThat(squat.lastMaxLoad().kilograms()).isEqualByComparingTo("70");
        assertThat(squat.firstVolume().kilograms()).isEqualByComparingTo("1120");
        assertThat(squat.lastVolume().kilograms()).isEqualByComparingTo("560");
    }

    @Test
    void shouldReportZeroAdherenceAndNoDataWithoutRecords() {
        var report = calculator.report(CLIENT_ID, period, List.of());

        assertThat(report.adherence().percentage()).isEqualByComparingTo("0");
        assertThat(report.hasData()).isFalse();
        assertThat(report.exercises()).isEmpty();
    }

    @Test
    void shouldChartAnExerciseOldestSessionFirst() {
        var progress = calculator.evolution(SQUAT.exerciseId(), ProgressWindow.WEEKS_4, threeSessions());

        assertThat(progress.points()).extracting(point -> point.date())
                .containsExactly(TODAY.minusDays(14), TODAY.minusDays(7));
        assertThat(progress.points().getFirst().maxLoad().kilograms()).isEqualByComparingTo("65");
        assertThat(progress.points().getLast().volume().kilograms()).isEqualByComparingTo("560");
        assertThat(progress.enoughData()).isTrue();
    }

    @Test
    void shouldFlagAChartWithFewerThanTwoSessions() {
        var progress = calculator.evolution(BENCH_PRESS.exerciseId(), ProgressWindow.WEEKS_8, threeSessions());

        assertThat(progress.points()).hasSize(1);
        assertThat(progress.enoughData()).isFalse();
        assertThat(progress.window().weeks()).isEqualTo(8);
    }

    @Test
    void shouldReturnAnEmptyChartForAnExerciseNeverRecorded() {
        var progress = calculator.evolution(SQUAT.exerciseId(), ProgressWindow.WEEKS_4, List.of());

        assertThat(progress.points()).isEmpty();
        assertThat(progress.enoughData()).isFalse();
    }
}
