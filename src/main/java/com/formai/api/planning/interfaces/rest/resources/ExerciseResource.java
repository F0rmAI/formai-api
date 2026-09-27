package com.formai.api.planning.interfaces.rest.resources;

import java.util.UUID;

// equipment is null when the exercise has none.
public record ExerciseResource(UUID id, String name, String muscleGroup, String equipment, String status) { }
