package com.formai.api.tracking.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;

// lastSessionDate / lastSessionStatus: the latest session of this day of the current routine, or
// null when the client has not had one yet.
public record RoutineDayResource(int order, String label, List<ExerciseToPerformResource> exercises,
                                 LocalDate lastSessionDate, String lastSessionStatus) { }
