package com.formai.api.tracking.interfaces.rest.resources;

import java.util.List;

public record RoutineDayResource(int order, String label, List<ExerciseToPerformResource> exercises) { }
