package com.formai.api.planning.domain.model.events;

import java.util.UUID;

public record AssignmentPersonalized(UUID clientId, UUID routineId) { }
