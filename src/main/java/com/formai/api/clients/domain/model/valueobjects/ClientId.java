package com.formai.api.clients.domain.model.valueobjects;

import java.util.UUID;

public record ClientId(UUID value) {

    public ClientId {
        if (value == null) {
            throw new IllegalArgumentException("ClientId value must not be null");
        }
    }
}
