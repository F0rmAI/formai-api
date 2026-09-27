package com.formai.api.tracking.domain.exceptions;

public class WorkoutSessionNotFoundException extends RuntimeException {

    public WorkoutSessionNotFoundException() {
        super("Workout session not found");
    }
}
