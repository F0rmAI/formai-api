package com.formai.api.notifications.domain.repositories;

import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.valueobjects.NotificationId;
import com.formai.api.notifications.domain.model.valueobjects.NotificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(NotificationId id);

    List<Notification> findAllDue(Instant now, int limit);

    List<Notification> findAllByHolderIdAndStatus(String holderId, NotificationStatus status);
}
