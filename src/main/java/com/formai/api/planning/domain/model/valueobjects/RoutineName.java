package com.formai.api.planning.domain.model.valueobjects;

public record RoutineName(String value) {

    public RoutineName {
        if (value == null || value.isBlank() || value.strip().length() > 120) {
            throw new IllegalArgumentException("RoutineName must have between 1 and 120 characters");
        }
        value = value.strip();
    }
}
