package com.formai.api.notifications.application.internal.commandservices;

import com.formai.api.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.commands.SendNotificationCommand;
import com.formai.api.notifications.domain.model.valueobjects.NotificationStatus;
import com.formai.api.notifications.domain.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.formai.api.notifications.NotificationsTestData.EMAIL;
import static com.formai.api.notifications.NotificationsTestData.HOLDER_ID;
import static com.formai.api.notifications.NotificationsTestData.NOW;
import static com.formai.api.notifications.NotificationsTestData.passwordResetEmail;
import static com.formai.api.notifications.NotificationsTestData.scheduledPasswordResetEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceImplTest {

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    EmailDeliveryService emailDeliveryService;

    @InjectMocks
    NotificationCommandServiceImpl commandService;

    private void savesReturnTheNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void shouldScheduleTheNotification() {
        when(notificationRepository.findAllByHolderIdAndStatus(HOLDER_ID, NotificationStatus.SCHEDULED)).thenReturn(List.of());
        savesReturnTheNotification();

        var notification = commandService.handle(passwordResetEmail(NOW)).orElseThrow();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);
        assertThat(notification.getDestination()).isEqualTo(EMAIL);
    }

    @Test
    void shouldCancelThePreviousPendingNotificationOfTheSameType() {
        // Arrange
        var previous = scheduledPasswordResetEmail();
        when(notificationRepository.findAllByHolderIdAndStatus(HOLDER_ID, NotificationStatus.SCHEDULED))
                .thenReturn(List.of(previous));
        savesReturnTheNotification();

        // Act
        var latest = commandService.handle(passwordResetEmail(NOW.plusSeconds(30))).orElseThrow();

        // Assert
        assertThat(previous.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        verify(notificationRepository).save(previous);
        assertThat(latest.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);
    }

    @Test
    void shouldMarkTheNotificationSentWhenTheEmailIsDelivered() {
        var notification = scheduledPasswordResetEmail();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(emailDeliveryService.send(notification.getDestination(), notification.getSubject(), notification.getBody()))
                .thenReturn(true);

        commandService.handle(new SendNotificationCommand(notification.getId()));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldMarkTheNotificationFailedWhenTheProviderRejectsIt() {
        var notification = scheduledPasswordResetEmail();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(emailDeliveryService.send(anyString(), anyString(), anyString())).thenReturn(false);

        commandService.handle(new SendNotificationCommand(notification.getId()));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldMarkTheNotificationFailedWhenTheProviderThrows() {
        var notification = scheduledPasswordResetEmail();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(emailDeliveryService.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("down"));

        commandService.handle(new SendNotificationCommand(notification.getId()));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void shouldNotSendANotificationThatIsNoLongerDue() {
        var notification = scheduledPasswordResetEmail();
        notification.markSent(Instant.parse("2026-01-15T15:00:05Z"));
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        commandService.handle(new SendNotificationCommand(notification.getId()));

        verify(emailDeliveryService, never()).send(anyString(), anyString(), anyString());
        verify(notificationRepository, never()).save(any());
    }
}
