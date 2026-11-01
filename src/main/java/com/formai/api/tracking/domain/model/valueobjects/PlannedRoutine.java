package com.formai.api.tracking.domain.model.valueobjects;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

// days carry the client's personalized prescriptions; trainerNote is null when the trainer left none.
public record PlannedRoutine(RoutineId routineId,
                             String routineName,
                             int version,
                             LocalDate startDate,
                             Set<DayOfWeek> trainingDays,
                             List<RoutineDay> days,
                             String trainerNote) {

    public PlannedRoutine {
        trainingDays = Set.copyOf(trainingDays);
        days = List.copyOf(days);
    }

    public PlannedRoutine(RoutineId routineId, String routineName, int version, LocalDate startDate,
                          Set<DayOfWeek> trainingDays, List<RoutineDay> days) {
        this(routineId, routineName, version, startDate, trainingDays, days, null);
    }
}
