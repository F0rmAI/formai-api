package com.formai.api.tracking.domain.model.valueobjects;

import java.util.UUID;

public record ClientId(UUID value) {

    public ClientId {
        if (value == null) {
            throw new IllegalArgumentException("ClientId value must not be null");
        }
    }
}
