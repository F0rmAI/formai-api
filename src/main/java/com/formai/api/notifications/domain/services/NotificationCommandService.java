package com.formai.api.notifications.domain.services;

import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.commands.ScheduleNotificationCommand;
import com.formai.api.notifications.domain.model.commands.SendNotificationCommand;

import java.util.Optional;

public interface NotificationCommandService {

    Optional<Notification> handle(ScheduleNotificationCommand command);

    void handle(SendNotificationCommand command);
}
