package com.formai.api.planning.domain.model.events;

import java.time.LocalDate;
import java.util.UUID;

public record AssignmentClosed(UUID clientId, UUID routineId, LocalDate endDate) { }
