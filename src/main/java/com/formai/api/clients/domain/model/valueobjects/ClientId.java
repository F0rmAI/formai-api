package com.formai.api.clients.domain.model.valueobjects;

import java.util.UUID;

// The id of the client's iam account: also the sub of their JWT in the app.
public record ClientId(UUID value) {

    public ClientId {
        if (value == null) {
            throw new IllegalArgumentException("ClientId value must not be null");
        }
    }
}
