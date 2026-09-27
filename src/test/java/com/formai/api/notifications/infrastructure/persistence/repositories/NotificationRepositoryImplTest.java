package com.formai.api.notifications.infrastructure.persistence.repositories;

import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.valueobjects.NotificationStatus;
import com.formai.api.notifications.infrastructure.persistence.entities.NotificationJpaEntity;
import com.formai.api.notifications.infrastructure.persistence.transform.NotificationJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.formai.api.notifications.NotificationsTestData.HOLDER_ID;
import static com.formai.api.notifications.NotificationsTestData.NOW;
import static com.formai.api.notifications.NotificationsTestData.scheduledPasswordResetEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryImplTest {

    @Mock
    NotificationJpaRepository jpaRepository;

    @Mock
    NotificationJpaMapper mapper;

    @InjectMocks
    NotificationRepositoryImpl repository;

    @Test
    void shouldMapToEntitySaveAndMapBackWhenSaving() {
        // Arrange
        var notification = new Notification();
        var entity = new NotificationJpaEntity();
        var savedEntity = new NotificationJpaEntity();
        var savedNotification = new Notification();
        when(mapper.toEntity(notification)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedNotification);

        // Act & Assert
        assertThat(repository.save(notification)).isSameAs(savedNotification);
    }

    @Test
    void shouldFindANotificationById() {
        var notification = scheduledPasswordResetEmail();
        var entity = new NotificationJpaEntity();
        when(jpaRepository.findById(notification.getId().value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(notification);

        assertThat(repository.findById(notification.getId())).contains(notification);
    }

    @Test
    void shouldFindTheDueNotifications() {
        var entity = new NotificationJpaEntity();
        var notification = scheduledPasswordResetEmail();
        when(jpaRepository.findAllDue(NOW, 50)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(notification);

        assertThat(repository.findAllDue(NOW, 50)).containsExactly(notification);
    }

    @Test
    void shouldQueryTheStatusByItsName() {
        var entity = new NotificationJpaEntity();
        var notification = scheduledPasswordResetEmail();
        when(jpaRepository.findAllByHolderIdAndStatus(HOLDER_ID, "SCHEDULED")).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(notification);

        assertThat(repository.findAllByHolderIdAndStatus(HOLDER_ID, NotificationStatus.SCHEDULED))
                .containsExactly(notification);
    }
}
