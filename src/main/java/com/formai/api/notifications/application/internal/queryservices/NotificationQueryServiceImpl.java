package com.formai.api.notifications.application.internal.queryservices;

import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.queries.GetDueNotificationsQuery;
import com.formai.api.notifications.domain.repositories.NotificationRepository;
import com.formai.api.notifications.domain.services.NotificationQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> handle(GetDueNotificationsQuery query) {
        return notificationRepository.findAllDue(query.now(), query.limit());
    }
}
