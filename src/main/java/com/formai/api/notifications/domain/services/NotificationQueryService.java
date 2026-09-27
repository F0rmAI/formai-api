package com.formai.api.notifications.domain.services;

import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.queries.GetDueNotificationsQuery;

import java.util.List;

public interface NotificationQueryService {

    List<Notification> handle(GetDueNotificationsQuery query);
}
