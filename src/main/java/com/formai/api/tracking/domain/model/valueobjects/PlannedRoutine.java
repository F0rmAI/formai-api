package com.formai.api.tracking.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.List;

// The routine currently assigned to a client, as planning describes it. Only the ACL
// (ExternalPlanningService) builds it, translating planning's published snapshot.
public record PlannedRoutine(RoutineId routineId,
                             String routineName,
                             int version,
                             LocalDate startDate,
                             List<RoutineDay> days) {

    public PlannedRoutine {
        days = List.copyOf(days);
    }
}
