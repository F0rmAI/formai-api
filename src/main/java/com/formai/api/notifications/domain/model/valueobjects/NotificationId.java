package com.formai.api.notifications.domain.model.valueobjects;

import java.util.UUID;

public record NotificationId(UUID value) {

    public NotificationId {
        if (value == null) {
            throw new IllegalArgumentException("NotificationId value must not be null");
        }
    }
}
