package com.formai.api.planning.domain.model.entities;

import java.util.List;

public class RoutineSession {

    private final int order;
    private final String label;
    private final List<PrescribedExercise> exercises;

    public RoutineSession(int order, String label, List<PrescribedExercise> exercises) {
        this.order = order;
        this.label = label;
        this.exercises = List.copyOf(exercises);
    }

    public int getOrder() {
        return order;
    }

    public String getLabel() {
        return label;
    }

    public List<PrescribedExercise> getExercises() {
        return exercises;
    }
}
