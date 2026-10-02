package com.formai.api.notifications.application.internal.jobs;

import com.formai.api.notifications.domain.model.commands.SendNotificationCommand;
import com.formai.api.notifications.domain.model.queries.GetDueNotificationsQuery;
import com.formai.api.notifications.domain.services.NotificationCommandService;
import com.formai.api.notifications.domain.services.NotificationQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.formai.api.notifications.NotificationsTestData.scheduledPasswordResetEmail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingNotificationDispatcherJobTest {

    @Mock
    NotificationQueryService notificationQueryService;

    @Mock
    NotificationCommandService notificationCommandService;

    @InjectMocks
    PendingNotificationDispatcherJob job;

    @Test
    void shouldSendEveryDueNotificationEvenWhenOneFails() {
        // Arrange
        var failing = scheduledPasswordResetEmail();
        var next = scheduledPasswordResetEmail();
        when(notificationQueryService.handle(any(GetDueNotificationsQuery.class))).thenReturn(List.of(failing, next));
        doThrow(new IllegalStateException("database down"))
                .when(notificationCommandService).handle(new SendNotificationCommand(failing.getId()));

        // Act
        job.run();

        // Assert
        verify(notificationCommandService).handle(new SendNotificationCommand(next.getId()));
    }
}
