package com.formai.api.notifications.application.internal.eventhandlers;

import com.formai.api.iam.domain.model.events.PasswordResetRequested;
import com.formai.api.notifications.domain.model.commands.ScheduleNotificationCommand;
import com.formai.api.notifications.domain.model.valueobjects.Channel;
import com.formai.api.notifications.domain.model.valueobjects.NotificationType;
import com.formai.api.notifications.domain.services.NotificationCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static com.formai.api.notifications.NotificationsTestData.EMAIL;
import static com.formai.api.notifications.NotificationsTestData.HOLDER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetRequestedEventHandlerTest {

    private static final String RESET_URL = "http://localhost:5173/password-reset?token=";

    @Mock
    NotificationCommandService notificationCommandService;

    @Test
    void shouldScheduleThePasswordResetEmailWithTheLink() {
        // Arrange
        var handler = new PasswordResetRequestedEventHandler(notificationCommandService, RESET_URL);
        var event = new PasswordResetRequested(UUID.fromString(HOLDER_ID), HOLDER_ID, EMAIL, "raw-token",
                Instant.parse("2026-10-01T15:30:00Z"));
        var command = ArgumentCaptor.forClass(ScheduleNotificationCommand.class);

        // Act
        handler.on(event);

        // Assert
        verify(notificationCommandService).handle(command.capture());
        assertThat(command.getValue().holderId()).isEqualTo(HOLDER_ID);
        assertThat(command.getValue().channel()).isEqualTo(Channel.EMAIL);
        assertThat(command.getValue().type()).isEqualTo(NotificationType.PASSWORD_RESET);
        assertThat(command.getValue().destination()).isEqualTo(EMAIL);
        assertThat(command.getValue().body()).contains(RESET_URL + "raw-token").contains("2026-10-01T15:30:00Z");
    }
}
