package com.formai.api.planning.domain.model.valueobjects;

public record MuscleGroup(String value) {

    public MuscleGroup {
        if (value == null || value.isBlank() || value.strip().length() > 60) {
            throw new IllegalArgumentException("MuscleGroup must have between 1 and 60 characters");
        }
        value = value.strip();
    }
}
