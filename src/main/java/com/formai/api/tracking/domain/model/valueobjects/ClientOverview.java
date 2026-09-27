package com.formai.api.tracking.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Optional;

// One row of the trainer's client list: the client (clients) with their current routine and
// the date they last trained (tracking).
public record ClientOverview(ClientId clientId,
                             String fullName,
                             String status,
                             Optional<String> activeRoutineName,
                             Optional<LocalDate> lastWorkoutOn) { }
