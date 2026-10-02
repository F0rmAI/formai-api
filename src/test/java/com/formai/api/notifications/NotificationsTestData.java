package com.formai.api.notifications;

import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.commands.ScheduleNotificationCommand;
import com.formai.api.notifications.domain.model.valueobjects.Channel;
import com.formai.api.notifications.domain.model.valueobjects.NotificationType;

import java.time.Instant;

public final class NotificationsTestData {

    public static final String HOLDER_ID = "11111111-1111-1111-1111-111111111111";
    public static final String EMAIL = "ana@formai.pe";
    public static final Instant NOW = Instant.parse("2026-01-15T15:00:00Z");

    private NotificationsTestData() {
    }

    public static ScheduleNotificationCommand passwordResetEmail(Instant scheduledAt) {
        return new ScheduleNotificationCommand(HOLDER_ID, Channel.EMAIL, NotificationType.PASSWORD_RESET, EMAIL,
                "Reset your FormAI password", "Use this link: http://localhost/reset?token=abc", scheduledAt);
    }

    public static Notification scheduledPasswordResetEmail() {
        return Notification.schedule(passwordResetEmail(NOW));
    }
}
