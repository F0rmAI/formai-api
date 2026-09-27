package com.formai.api.notifications.domain.model.commands;

import com.formai.api.notifications.domain.model.valueobjects.NotificationId;

public record SendNotificationCommand(NotificationId notificationId) { }
