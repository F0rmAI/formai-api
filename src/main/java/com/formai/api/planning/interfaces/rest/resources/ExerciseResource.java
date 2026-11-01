package com.formai.api.planning.interfaces.rest.resources;

import java.util.UUID;

// origin: GLOBAL (shared catalog, read-only) or OWN; routineCount: the trainer's routines that prescribe it.
public record ExerciseResource(UUID id, String name, String muscleGroup, String equipment, UUID machineId,
                               String status, String origin, long routineCount) { }
