package com.formai.api.planning.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.UUID;

// endDate is null while the assignment is current.
public record AssignmentResource(UUID clientId,
                                 UUID routineId,
                                 String routineName,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 boolean current) { }
