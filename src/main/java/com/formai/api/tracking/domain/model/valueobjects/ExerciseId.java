package com.formai.api.tracking.domain.model.valueobjects;

import java.util.UUID;

public record ExerciseId(UUID value) {

    public ExerciseId {
        if (value == null) {
            throw new IllegalArgumentException("ExerciseId value must not be null");
        }
    }
}
