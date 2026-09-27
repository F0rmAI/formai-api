package com.formai.api.notifications.application.internal.jobs;

import com.formai.api.notifications.domain.model.commands.SendNotificationCommand;
import com.formai.api.notifications.domain.model.queries.GetDueNotificationsQuery;
import com.formai.api.notifications.domain.services.NotificationCommandService;
import com.formai.api.notifications.domain.services.NotificationQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PendingNotificationDispatcherJob {

    private static final Logger log = LoggerFactory.getLogger(PendingNotificationDispatcherJob.class);
    private static final int BATCH_SIZE = 50;

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;

    public PendingNotificationDispatcherJob(NotificationQueryService notificationQueryService,
                                            NotificationCommandService notificationCommandService) {
        this.notificationQueryService = notificationQueryService;
        this.notificationCommandService = notificationCommandService;
    }

    @Scheduled(fixedDelayString = "${notifications.jobs.dispatcher.delay}")
    public void run() {
        for (var notification : notificationQueryService.handle(new GetDueNotificationsQuery(Instant.now(), BATCH_SIZE))) {
            try {
                notificationCommandService.handle(new SendNotificationCommand(notification.getId()));
            } catch (RuntimeException ex) {
                log.error("Could not dispatch notification {}", notification.getId().value(), ex);
            }
        }
    }
}
