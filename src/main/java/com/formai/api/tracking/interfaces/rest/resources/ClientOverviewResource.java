package com.formai.api.tracking.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.UUID;

// activeRoutineName and lastWorkoutOn are null when the client has no current routine or
// has not trained yet.
public record ClientOverviewResource(UUID clientId,
                                     String fullName,
                                     String status,
                                     String activeRoutineName,
                                     LocalDate lastWorkoutOn) { }
