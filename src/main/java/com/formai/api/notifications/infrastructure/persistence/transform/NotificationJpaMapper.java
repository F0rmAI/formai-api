package com.formai.api.notifications.infrastructure.persistence.transform;

import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.valueobjects.NotificationId;
import com.formai.api.notifications.infrastructure.persistence.entities.NotificationJpaEntity;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface NotificationJpaMapper {

    NotificationJpaEntity toEntity(Notification notification);

    Notification toDomain(NotificationJpaEntity entity);

    // required by MapStruct: single-field VOs and Optional fields need an explicit converter.
    default UUID map(NotificationId id) {
        return id == null ? null : id.value();
    }

    default NotificationId mapNotificationId(UUID value) {
        return value == null ? null : new NotificationId(value);
    }

    default Instant map(Optional<Instant> value) {
        return value == null ? null : value.orElse(null);
    }
}
