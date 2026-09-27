package com.formai.api.tracking;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseToPerform;
import com.formai.api.tracking.domain.model.valueobjects.PlannedRoutine;
import com.formai.api.tracking.domain.model.valueobjects.RoutineDay;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Shared fixtures for the tracking tests: a two-day routine (legs, then chest) assigned
// to one client.
public final class TrackingTestData {

    // Holder ids are JWT subjects; a client's subject is also its ClientId.
    public static final String CLIENT_HOLDER_ID = "11111111-1111-1111-1111-111111111111";
    public static final ClientId CLIENT_ID = new ClientId(UUID.fromString(CLIENT_HOLDER_ID));
    public static final String TRAINER_HOLDER_ID = "22222222-2222-2222-2222-222222222222";
    public static final RoutineId ROUTINE_ID = new RoutineId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    public static final LocalDate START_DATE = LocalDate.of(2026, 9, 1);
    public static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    public static final ExerciseToPerform SQUAT = new ExerciseToPerform(
            new ExerciseId(UUID.fromString("44444444-4444-4444-4444-444444444444")), "Squat", 3, 10,
            new BigDecimal("60.00"), 90);
    public static final ExerciseToPerform BENCH_PRESS = new ExerciseToPerform(
            new ExerciseId(UUID.fromString("55555555-5555-5555-5555-555555555555")), "Bench press", 2, 8,
            new BigDecimal("40.00"), 90);
    public static final ExerciseToPerform DEADLIFT = new ExerciseToPerform(
            new ExerciseId(UUID.fromString("66666666-6666-6666-6666-666666666666")), "Deadlift", 2, 5,
            new BigDecimal("100.00"), 120);

    public static final RoutineDay LEGS_DAY = new RoutineDay(1, "Day A · Legs", List.of(SQUAT, BENCH_PRESS));
    public static final RoutineDay BACK_DAY = new RoutineDay(2, "Day B · Back", List.of(DEADLIFT));

    private TrackingTestData() {
    }

    public static PlannedRoutine plannedRoutine(int version) {
        return new PlannedRoutine(ROUTINE_ID, "Strength 12 weeks", version, START_DATE, List.of(LEGS_DAY, BACK_DAY));
    }

    public static ActiveRoutine activeRoutine() {
        return ActiveRoutine.syncFrom(new SyncActiveRoutineCommand(CLIENT_ID), plannedRoutine(1));
    }

    // A pending legs-day session: squat (3 sets) and bench press (2 sets).
    public static WorkoutSession pendingSession(LocalDate date) {
        return WorkoutSession.schedule(new ScheduleWorkoutSessionCommand(CLIENT_ID, date), activeRoutine(), LEGS_DAY);
    }
}
