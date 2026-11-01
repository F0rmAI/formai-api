package com.formai.api.planning.domain.model.valueobjects;

import java.util.List;

public record MuscleGroup(String value) {

    // The muscle groups a new exercise can be filed under, the same ones the global catalog uses.
    // Exercises created before the list existed keep the value they were saved with.
    public static final List<MuscleGroup> STANDARD = List.of(
            new MuscleGroup("Pectoral"),
            new MuscleGroup("Espalda"),
            new MuscleGroup("Hombros"),
            new MuscleGroup("Bíceps"),
            new MuscleGroup("Tríceps"),
            new MuscleGroup("Core"),
            new MuscleGroup("Glúteos"),
            new MuscleGroup("Cuádriceps"),
            new MuscleGroup("Isquiotibiales"),
            new MuscleGroup("Pantorrillas"),
            new MuscleGroup("Cuerpo completo"));

    public MuscleGroup {
        if (value == null || value.isBlank() || value.strip().length() > 60) {
            throw new IllegalArgumentException("MuscleGroup must have between 1 and 60 characters");
        }
        value = value.strip();
    }

    public boolean isStandard() {
        return STANDARD.stream().anyMatch(group -> group.value.equalsIgnoreCase(value));
    }
}
