package com.formai.api.tracking.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.UUID;

public record ClientOverviewResource(UUID clientId,
                                     String fullName,
                                     String status,
                                     String activeRoutineName,
                                     LocalDate lastWorkoutOn) { }
