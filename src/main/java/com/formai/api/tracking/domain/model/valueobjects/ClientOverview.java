package com.formai.api.tracking.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Optional;

public record ClientOverview(ClientId clientId,
                             String fullName,
                             String status,
                             Optional<String> activeRoutineName,
                             Optional<LocalDate> lastWorkoutOn) { }
