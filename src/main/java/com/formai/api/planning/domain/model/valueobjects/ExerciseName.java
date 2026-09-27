package com.formai.api.planning.domain.model.valueobjects;

public record ExerciseName(String value) {

    public ExerciseName {
        if (value == null || value.isBlank() || value.strip().length() > 120) {
            throw new IllegalArgumentException("ExerciseName must have between 1 and 120 characters");
        }
        value = value.strip();
    }
}
