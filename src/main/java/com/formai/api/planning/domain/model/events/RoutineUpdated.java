package com.formai.api.planning.domain.model.events;

import java.util.UUID;

public record RoutineUpdated(UUID routineId, int version) { }
