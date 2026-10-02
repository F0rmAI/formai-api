package com.formai.api.planning;

import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.commands.CreateRoutineCommand;
import com.formai.api.planning.domain.model.entities.PrescribedExercise;
import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.ExerciseName;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.domain.model.valueobjects.Prescription;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlanningTestData {

    public static final String TRAINER_HOLDER_ID = "22222222-2222-2222-2222-222222222222";
    public static final ClientId CLIENT_ID = new ClientId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    public static final ExerciseId SQUAT_ID = new ExerciseId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
    public static final ExerciseId DEADLIFT_ID = new ExerciseId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
    public static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);

    private PlanningTestData() {
    }

    public static Exercise squat() {
        var exercise = Exercise.create(new CreateExerciseCommand(TRAINER_HOLDER_ID, new ExerciseName("Squat"),
                new MuscleGroup("Legs"), Optional.of("Barbell")));
        exercise.setId(SQUAT_ID);
        return exercise;
    }

    public static Prescription prescription(int sets, int reps, String targetLoadKg) {
        return new Prescription(sets, reps, new BigDecimal(targetLoadKg), 90);
    }

    public static List<RoutineSession> twoSessions() {
        return List.of(
                new RoutineSession(1, "Day A · Legs",
                        List.of(new PrescribedExercise(SQUAT_ID, "Squat", prescription(3, 10, "60")))),
                new RoutineSession(2, "Day B · Back",
                        List.of(new PrescribedExercise(DEADLIFT_ID, "Deadlift", prescription(2, 5, "100")))));
    }

    public static Routine routine() {
        return Routine.create(new CreateRoutineCommand(TRAINER_HOLDER_ID, new RoutineName("Strength 12 weeks"),
                twoSessions()));
    }

    public static ClientPlan emptyPlan() {
        return ClientPlan.startFor(CLIENT_ID, TRAINER_HOLDER_ID);
    }
}
