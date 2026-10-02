package com.formai.api.tracking.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ActiveRoutineResource(UUID routineId,
                                    String routineName,
                                    int version,
                                    LocalDate startDate,
                                    List<String> trainingDays,
                                    Integer todaySessionOrder,
                                    UUID todayWorkoutSessionId,
                                    List<RoutineDayResource> sessions) { }
