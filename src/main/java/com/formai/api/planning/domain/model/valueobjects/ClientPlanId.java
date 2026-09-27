package com.formai.api.planning.domain.model.valueobjects;

import java.util.UUID;

public record ClientPlanId(UUID value) {

    public ClientPlanId {
        if (value == null) {
            throw new IllegalArgumentException("ClientPlanId value must not be null");
        }
    }
}
