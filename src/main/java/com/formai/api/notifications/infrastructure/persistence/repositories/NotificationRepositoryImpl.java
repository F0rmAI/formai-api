package com.formai.api.notifications.infrastructure.persistence.repositories;

import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.valueobjects.NotificationId;
import com.formai.api.notifications.domain.model.valueobjects.NotificationStatus;
import com.formai.api.notifications.domain.repositories.NotificationRepository;
import com.formai.api.notifications.infrastructure.persistence.transform.NotificationJpaMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationJpaMapper mapper;

    public NotificationRepositoryImpl(NotificationJpaRepository jpaRepository, NotificationJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notification save(Notification notification) {
        var entity = mapper.toEntity(notification);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findAllDue(Instant now, int limit) {
        return jpaRepository.findAllDue(now, Notification.MAX_ATTEMPTS, limit).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Notification> findAllByHolderIdAndStatus(String holderId, NotificationStatus status) {
        return jpaRepository.findAllByHolderIdAndStatus(holderId, status.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
