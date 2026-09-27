package com.formai.api.tracking.domain.model.valueobjects;

import java.util.UUID;

public record ActiveRoutineId(UUID value) {

    public ActiveRoutineId {
        if (value == null) {
            throw new IllegalArgumentException("ActiveRoutineId value must not be null");
        }
    }
}
