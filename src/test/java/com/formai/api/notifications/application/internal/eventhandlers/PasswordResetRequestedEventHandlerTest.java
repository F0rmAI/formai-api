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
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.UUID;

import static com.formai.api.notifications.NotificationsTestData.EMAIL;
import static com.formai.api.notifications.NotificationsTestData.HOLDER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetRequestedEventHandlerTest {

    private static final String RESET_URL = "http://localhost:5173/password-reset?token=";

    @Mock
    NotificationCommandService notificationCommandService;

    @Mock
    PlatformTransactionManager transactionManager;

    private static PasswordResetRequested resetRequested() {
        return new PasswordResetRequested(UUID.fromString(HOLDER_ID), HOLDER_ID, EMAIL, "raw-token",
                Instant.parse("2026-10-01T15:30:00Z"));
    }

    @Test
    void shouldScheduleThePasswordResetEmailWithTheLink() {
        // Arrange
        var handler = new PasswordResetRequestedEventHandler(notificationCommandService, transactionManager, RESET_URL);
        var event = resetRequested();
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

    @Test
    void shouldKeepTheResetRequestNeutralWhenTheEmailCannotBeScheduled() {
        when(notificationCommandService.handle(any(ScheduleNotificationCommand.class)))
                .thenThrow(new IllegalStateException("database unavailable"));
        var handler = new PasswordResetRequestedEventHandler(notificationCommandService, transactionManager, RESET_URL);

        assertThatCode(() -> handler.on(resetRequested())).doesNotThrowAnyException();
    }
}
