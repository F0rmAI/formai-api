package com.formai.api.planning.interfaces.rest.resources;

import java.util.List;

public record RoutineSessionResource(int order, String label, List<PrescribedExerciseResource> exercises) { }
