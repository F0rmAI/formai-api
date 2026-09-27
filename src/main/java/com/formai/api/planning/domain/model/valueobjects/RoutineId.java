package com.formai.api.planning.domain.model.valueobjects;

import java.util.UUID;

public record RoutineId(UUID value) {

    public RoutineId {
        if (value == null) {
            throw new IllegalArgumentException("RoutineId value must not be null");
        }
    }
}
