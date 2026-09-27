package com.formai.api.notifications.domain.model.commands;

import com.formai.api.notifications.domain.model.valueobjects.Channel;
import com.formai.api.notifications.domain.model.valueobjects.NotificationType;

import java.time.Instant;

public record ScheduleNotificationCommand(String holderId, Channel channel, NotificationType type, String destination,
                                          String subject, String body, Instant scheduledAt) { }
