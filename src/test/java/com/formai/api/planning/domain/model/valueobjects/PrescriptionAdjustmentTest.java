package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.exceptions.InvalidAdjustmentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.formai.api.planning.PlanningTestData.DEADLIFT_ID;
import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.prescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionAdjustmentTest {

    @Test
    void shouldChangeOnlyTheValuesItCarries() {
        var adjusted = new PrescriptionAdjustment(1, SQUAT_ID, new BigDecimal("45"), null)
                .applyTo(prescription(3, 10, "60"));

        assertThat(adjusted.sets()).isEqualTo(3);
        assertThat(adjusted.reps()).isEqualTo(10);
        assertThat(adjusted.targetLoadKg()).isEqualByComparingTo("45");
        assertThat(adjusted.restSeconds()).isEqualTo(90);
    }

    @Test
    void shouldPersonalizeOnlyTheMatchingExerciseOfTheSession() {
        var adjustments = List.of(new PrescriptionAdjustment(1, SQUAT_ID, null, 150));

        assertThat(PrescriptionAdjustment.personalize(adjustments, 1, SQUAT_ID, prescription(3, 10, "60"))
                .restSeconds()).isEqualTo(150);
        assertThat(PrescriptionAdjustment.personalize(adjustments, 2, SQUAT_ID, prescription(3, 10, "60"))
                .restSeconds()).isEqualTo(90);
        assertThat(PrescriptionAdjustment.personalize(adjustments, 1, DEADLIFT_ID, prescription(3, 10, "60"))
                .restSeconds()).isEqualTo(90);
    }

    @Test
    void shouldRejectAnAdjustmentWithNothingToChange() {
        assertThatThrownBy(() -> new PrescriptionAdjustment(1, SQUAT_ID, null, null))
                .isInstanceOf(InvalidAdjustmentException.class);
    }

    @Test
    void shouldRejectNegativeValues() {
        assertThatThrownBy(() -> new PrescriptionAdjustment(1, SQUAT_ID, new BigDecimal("-1"), null))
                .isInstanceOf(InvalidAdjustmentException.class);
        assertThatThrownBy(() -> new PrescriptionAdjustment(1, SQUAT_ID, null, -5))
                .isInstanceOf(InvalidAdjustmentException.class);
    }

    @Test
    void shouldRejectANoteLongerThanTheLimit() {
        assertThatThrownBy(() -> new AssignmentNote("x".repeat(501))).isInstanceOf(InvalidAdjustmentException.class);
        assertThat(new AssignmentNote("x".repeat(500)).value()).hasSize(500);
    }
}
