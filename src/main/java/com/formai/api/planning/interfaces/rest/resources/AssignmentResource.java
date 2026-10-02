package com.formai.api.planning.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssignmentResource(UUID clientId,
                                 UUID routineId,
                                 String routineName,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 List<String> trainingDays,
                                 boolean current) { }
