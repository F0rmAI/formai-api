package com.formai.api.planning.domain.model.valueobjects;

import java.util.UUID;

public record MachineId(UUID value) {

    public MachineId {
        if (value == null) {
            throw new IllegalArgumentException("MachineId value must not be null");
        }
    }
}
