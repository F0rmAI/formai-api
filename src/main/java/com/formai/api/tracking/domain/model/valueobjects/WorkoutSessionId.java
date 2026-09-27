package com.formai.api.tracking.domain.model.valueobjects;

import java.util.UUID;

public record WorkoutSessionId(UUID value) {

    public WorkoutSessionId {
        if (value == null) {
            throw new IllegalArgumentException("WorkoutSessionId value must not be null");
        }
    }
}
