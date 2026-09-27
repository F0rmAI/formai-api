package com.formai.api.notifications.domain.model.aggregates;

import com.formai.api.notifications.domain.model.valueobjects.NotificationStatus;
import org.junit.jupiter.api.Test;

import static com.formai.api.notifications.NotificationsTestData.NOW;
import static com.formai.api.notifications.NotificationsTestData.passwordResetEmail;
import static com.formai.api.notifications.NotificationsTestData.scheduledPasswordResetEmail;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    void shouldScheduleWithoutAttempts() {
        var notification = scheduledPasswordResetEmail();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SCHEDULED);
        assertThat(notification.getAttempts()).isZero();
        assertThat(notification.getSentAt()).isEmpty();
    }

    @Test
    void shouldBeDueOnceItsTimeHasCome() {
        var notification = scheduledPasswordResetEmail();

        assertThat(notification.isDue(NOW)).isTrue();
        assertThat(notification.isDue(NOW.minusSeconds(1))).isFalse();
    }

    @Test
    void shouldNotBeDueBeforeItsScheduledTime() {
        var notification = Notification.schedule(passwordResetEmail(NOW.plusSeconds(60)));

        assertThat(notification.isDue(NOW)).isFalse();
    }

    @Test
    void shouldStopBeingDueOnceSent() {
        var notification = scheduledPasswordResetEmail();

        notification.markSent(NOW);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).contains(NOW);
        assertThat(notification.getAttempts()).isEqualTo(1);
        assertThat(notification.isDue(NOW)).isFalse();
    }

    @Test
    void shouldStayDueAfterAFailedDeliverySoItIsRetried() {
        var notification = scheduledPasswordResetEmail();

        notification.markFailed();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getAttempts()).isEqualTo(1);
        assertThat(notification.isDue(NOW)).isTrue();
    }

    @Test
    void shouldNeverBeDueOnceCancelled() {
        var notification = scheduledPasswordResetEmail();

        notification.cancel();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        assertThat(notification.isDue(NOW)).isFalse();
    }
}
