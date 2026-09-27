package com.formai.api.notifications.application.internal.queryservices;

import com.formai.api.notifications.domain.model.queries.GetDueNotificationsQuery;
import com.formai.api.notifications.domain.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.formai.api.notifications.NotificationsTestData.NOW;
import static com.formai.api.notifications.NotificationsTestData.scheduledPasswordResetEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceImplTest {

    @Mock
    NotificationRepository notificationRepository;

    @InjectMocks
    NotificationQueryServiceImpl queryService;

    @Test
    void shouldReturnTheDueNotifications() {
        var due = scheduledPasswordResetEmail();
        when(notificationRepository.findAllDue(NOW, 50)).thenReturn(List.of(due));

        assertThat(queryService.handle(new GetDueNotificationsQuery(NOW, 50))).containsExactly(due);
    }
}
