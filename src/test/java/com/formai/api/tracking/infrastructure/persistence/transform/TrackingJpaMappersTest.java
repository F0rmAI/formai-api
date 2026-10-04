package com.formai.api.tracking.infrastructure.persistence.transform;

import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.valueobjects.ComplianceStatus;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.ZoneId;

import static com.formai.api.tracking.TrackingTestData.BACK_DAY;
import static com.formai.api.tracking.TrackingTestData.BENCH_PRESS;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.LEGS_DAY;
import static com.formai.api.tracking.TrackingTestData.SQUAT;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static com.formai.api.tracking.TrackingTestData.pendingSession;
import static org.assertj.core.api.Assertions.assertThat;

class TrackingJpaMappersTest {

    private final ActiveRoutineJpaMapper activeRoutineMapper = Mappers.getMapper(ActiveRoutineJpaMapper.class);
    private final WorkoutSessionJpaMapper workoutSessionMapper = Mappers.getMapper(WorkoutSessionJpaMapper.class);

    @Test
    void shouldStoreTheRoutineDaysAsJsonAndReadThemBack() {
        // Arrange
        var routine = activeRoutine();
        routine.end(new EndActiveRoutineCommand(CLIENT_ID, TODAY));
        routine.changeTimeZone(ZoneId.of("Asia/Kolkata"));
        routine.recordDailyRun(TODAY);

        // Act
        var entity = activeRoutineMapper.toEntity(routine);
        var restored = activeRoutineMapper.toDomain(entity);

        // Assert
        assertThat(entity.getClientId()).isEqualTo(CLIENT_ID.value());
        assertThat(entity.getDaysJson()).contains("Day A · Legs");
        assertThat(restored.getId()).isEqualTo(routine.getId());
        assertThat(restored.getRoutineId()).isEqualTo(routine.getRoutineId());
        assertThat(restored.getEndDate()).isEqualTo(TODAY);
        assertThat(restored.getDays()).containsExactly(LEGS_DAY, BACK_DAY);
        assertThat(entity.getTimeZone()).isEqualTo("Asia/Kolkata");
        assertThat(restored.getTimeZone()).isEqualTo(routine.getTimeZone());
        assertThat(restored.getLastDailyRunOn()).isEqualTo(TODAY);
    }

    @Test
    void shouldFlattenTheSetsAndRegroupThemUnderTheirExercise() {
        var session = pendingSession(TODAY);
        session.recordSet(new RecordSetCommand(session.getId(), CLIENT_ID, SQUAT.exerciseId(), 2,
                new Load(new BigDecimal("62.5")), new Reps(8)));
        session.recordSet(new RecordSetCommand(session.getId(), CLIENT_ID, SQUAT.exerciseId(), 1,
                new Load(new BigDecimal("60")), new Reps(10)));
        session.finish(new FinishWorkoutSessionCommand(session.getId(), CLIENT_ID, true));

        var entity = workoutSessionMapper.toEntity(session);
        var restored = workoutSessionMapper.toDomain(entity);

        assertThat(entity.getPlannedExercises()).hasSize(2);
        assertThat(entity.getSets()).hasSize(2);
        assertThat(entity.getStatus()).isEqualTo("PARTIAL");
        assertThat(restored.getId()).isEqualTo(session.getId());
        assertThat(restored.getStatus()).isEqualTo(ComplianceStatus.PARTIAL);
        assertThat(restored.getFinishedAt()).isEqualTo(session.getFinishedAt());
        assertThat(restored.getExercises()).extracting(exercise -> exercise.getToPerform())
                .containsExactly(SQUAT, BENCH_PRESS);
        var squatSets = restored.getExercises().getFirst().getSets();
        assertThat(squatSets).extracting(set -> set.getSetNumber()).containsExactly(1, 2);
        assertThat(squatSets.get(1).getLoad().kilograms()).isEqualByComparingTo("62.5");
        assertThat(restored.getExercises().get(1).getSets()).isEmpty();
        assertThat(restored.volume()).isEqualTo(session.volume());
    }
}
