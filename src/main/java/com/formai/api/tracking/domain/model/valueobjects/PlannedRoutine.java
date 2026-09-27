package com.formai.api.tracking.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.List;

public record PlannedRoutine(RoutineId routineId,
                             String routineName,
                             int version,
                             LocalDate startDate,
                             List<RoutineDay> days) {

    public PlannedRoutine {
        days = List.copyOf(days);
    }
}
