package com.formai.api.planning.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingDaysTest {

    @Test
    void shouldTrainEveryDayWhenNoDaysAreGiven() {
        assertThat(TrainingDays.ofNames(null).days()).hasSize(7);
        assertThat(TrainingDays.ofNames(List.of()).days()).hasSize(7);
    }

    @Test
    void shouldReadTheDayNamesWhateverTheirCaseAndListThemFromMonday() {
        var days = TrainingDays.ofNames(List.of("friday", "MONDAY", " Wednesday "));

        assertThat(days.names()).containsExactly("MONDAY", "WEDNESDAY", "FRIDAY");
        assertThat(days.includes(DayOfWeek.MONDAY)).isTrue();
        assertThat(days.includes(DayOfWeek.TUESDAY)).isFalse();
    }

    @Test
    void shouldRejectAnUnknownDay() {
        assertThatThrownBy(() -> TrainingDays.ofNames(List.of("FUNDAY")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectAnAssignmentWithoutTrainingDays() {
        assertThatThrownBy(() -> new TrainingDays(Set.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
