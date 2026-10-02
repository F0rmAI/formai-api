package com.formai.api.tracking.domain.model.valueobjects;

import java.util.List;

public record RoutineDay(int order, String label, List<ExerciseToPerform> exercises) {

    public RoutineDay {
        exercises = List.copyOf(exercises);
    }
}
