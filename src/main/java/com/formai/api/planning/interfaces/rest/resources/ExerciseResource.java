package com.formai.api.planning.interfaces.rest.resources;

import java.util.UUID;

public record ExerciseResource(UUID id, String name, String muscleGroup, String equipment, String status) { }
