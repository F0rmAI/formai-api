package com.formai.api.planning.infrastructure.persistence.transform;

import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.RoutineStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.START_DATE;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.emptyPlan;
import static com.formai.api.planning.PlanningTestData.routine;
import static com.formai.api.planning.PlanningTestData.squat;
import static com.formai.api.planning.PlanningTestData.twoSessions;
import static org.assertj.core.api.Assertions.assertThat;

class PlanningJpaMappersTest {

    private final ExerciseJpaMapper exerciseMapper = Mappers.getMapper(ExerciseJpaMapper.class);
    private final RoutineJpaMapper routineMapper = Mappers.getMapper(RoutineJpaMapper.class);
    private final ClientPlanJpaMapper clientPlanMapper = Mappers.getMapper(ClientPlanJpaMapper.class);

    @Test
    void shouldRestoreAnExercise() {
        // Arrange
        var exercise = squat();
        exercise.archive();

        // Act
        var entity = exerciseMapper.toEntity(exercise);
        var restored = exerciseMapper.toDomain(entity);

        // Assert
        assertThat(entity.getStatus()).isEqualTo("ARCHIVED");
        assertThat(restored.getId()).isEqualTo(SQUAT_ID);
        assertThat(restored.getName()).isEqualTo(exercise.getName());
        assertThat(restored.getEquipment()).isEqualTo("Barbell");
        assertThat(restored.getStatus()).isEqualTo(ExerciseStatus.ARCHIVED);
    }

    @Test
    void shouldStoreEveryVersionAsJsonAndReadItBack() {
        var routine = routine();
        routine.revise(new UpdateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID, routine.getName(),
                twoSessions().subList(0, 1)));
        routine.markActive();

        var entity = routineMapper.toEntity(routine);
        var restored = routineMapper.toDomain(entity);

        assertThat(entity.getVersions()).hasSize(2);
        assertThat(entity.getVersions().getFirst().getSessionsJson()).contains("Day A · Legs", SQUAT_ID.value().toString());
        assertThat(restored.getStatus()).isEqualTo(RoutineStatus.ACTIVE);
        assertThat(restored.getVersions()).hasSize(2);
        assertThat(restored.currentVersion().getNumber()).isEqualTo(2);
        assertThat(restored.currentVersion().getSessions()).hasSize(1);
        var squat = restored.getVersions().getFirst().getSessions().getFirst().getExercises().getFirst();
        assertThat(squat.getExerciseId()).isEqualTo(SQUAT_ID);
        assertThat(squat.getExerciseName()).isEqualTo("Squat");
        assertThat(squat.getPrescription()).isEqualTo(twoSessions().getFirst().getExercises().getFirst().getPrescription());
    }

    @Test
    void shouldKeepTheAssignmentHistory() {
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(new RoutineId(UUID.randomUUID()), CLIENT_ID, TRAINER_HOLDER_ID, START_DATE));
        plan.assign(new AssignRoutineCommand(new RoutineId(UUID.randomUUID()), CLIENT_ID, TRAINER_HOLDER_ID,
                START_DATE.plusDays(30)));

        var restored = clientPlanMapper.toDomain(clientPlanMapper.toEntity(plan));

        assertThat(restored.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(restored.getAssignments()).hasSize(2);
        assertThat(restored.getAssignments().getFirst().getPeriod().endDate()).isEqualTo(START_DATE.plusDays(29));
        assertThat(restored.currentAssignment()).hasValueSatisfying(current ->
                assertThat(current.getPeriod().startDate()).isEqualTo(START_DATE.plusDays(30)));
    }
}
