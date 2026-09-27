package com.formai.api.clients.domain.model.valueobjects;

import java.util.UUID;

public record TrainerId(UUID value) {

    public TrainerId {
        if (value == null) {
            throw new IllegalArgumentException("TrainerId value must not be null");
        }
    }
}
