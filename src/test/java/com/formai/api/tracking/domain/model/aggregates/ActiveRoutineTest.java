package com.formai.api.tracking.domain.model.aggregates;

import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.RoutineDay;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.formai.api.tracking.TrackingTestData.BACK_DAY;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.LEGS_DAY;
import static com.formai.api.tracking.TrackingTestData.ROUTINE_ID;
import static com.formai.api.tracking.TrackingTestData.START_DATE;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static com.formai.api.tracking.TrackingTestData.plannedRoutine;
import static org.assertj.core.api.Assertions.assertThat;

class ActiveRoutineTest {

    @Test
    void shouldCopyThePlannedRoutineWhenSyncing() {
        // Act
        var routine = activeRoutine();

        // Assert
        assertThat(routine.getId()).isNotNull();
        assertThat(routine.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(routine.getRoutineId()).isEqualTo(ROUTINE_ID);
        assertThat(routine.getRoutineName()).isEqualTo("Strength 12 weeks");
        assertThat(routine.getVersion()).isEqualTo(1);
        assertThat(routine.getStartDate()).isEqualTo(START_DATE);
        assertThat(routine.getEndDate()).isNull();
        assertThat(routine.getDays()).containsExactly(LEGS_DAY, BACK_DAY);
    }

    @Test
    void shouldStartWithTheFirstDayWhenNothingWasFinishedYet() {
        assertThat(activeRoutine().nextDay(Optional.empty())).isEqualTo(LEGS_DAY);
    }

    @Test
    void shouldFollowTheLastFinishedDayAndStartOverAfterTheLastOne() {
        var routine = activeRoutine();

        assertThat(routine.nextDay(Optional.of(1))).isEqualTo(BACK_DAY);
        assertThat(routine.nextDay(Optional.of(2))).isEqualTo(LEGS_DAY);
    }

    @Test
    void shouldFollowRoutineOrderWhateverOrderTheDaysAreStoredIn() {
        var routine = activeRoutine();
        routine.setDays(List.of(BACK_DAY, LEGS_DAY));

        assertThat(routine.nextDay(Optional.empty())).isEqualTo(LEGS_DAY);
    }

    @Test
    void shouldBeActiveFromItsStartDateUntilItsEndDateIncluded() {
        var routine = activeRoutine();
        routine.end(new EndActiveRoutineCommand(CLIENT_ID, START_DATE.plusDays(10)));

        assertThat(routine.isActiveOn(START_DATE.minusDays(1))).isFalse();
        assertThat(routine.isActiveOn(START_DATE)).isTrue();
        assertThat(routine.isActiveOn(START_DATE.plusDays(10))).isTrue();
        assertThat(routine.isActiveOn(START_DATE.plusDays(11))).isFalse();
    }

    @Test
    void shouldTakeTheNewVersionAndReopenWhenResynced() {
        var routine = activeRoutine();
        routine.end(new EndActiveRoutineCommand(CLIENT_ID, START_DATE.plusDays(3)));
        var plan = plannedRoutine(2);

        routine.resync(plan);

        assertThat(routine.getVersion()).isEqualTo(2);
        assertThat(routine.getEndDate()).isNull();
        assertThat(routine.getDays()).extracting(RoutineDay::label).containsExactly("Day A · Legs", "Day B · Back");
    }

    @Test
    void shouldTrainOnlyOnItsTrainingDaysWhileActive() {
        var routine = activeRoutine();
        routine.setTrainingDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        var monday = LocalDate.of(2026, 9, 28);

        assertThat(routine.trainsOn(monday)).isTrue();
        assertThat(routine.trainsOn(monday.plusDays(1))).isFalse();
        assertThat(routine.trainsOn(monday.plusDays(2))).isTrue();
        assertThat(routine.trainsOn(LocalDate.of(2026, 8, 31))).isFalse();   // a Monday before the start date
    }
}
