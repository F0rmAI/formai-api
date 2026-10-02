package com.formai.api.tracking.domain.model.valueobjects;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record PlannedRoutine(RoutineId routineId,
                             String routineName,
                             int version,
                             LocalDate startDate,
                             Set<DayOfWeek> trainingDays,
                             List<RoutineDay> days) {

    public PlannedRoutine {
        trainingDays = Set.copyOf(trainingDays);
        days = List.copyOf(days);
    }
}
