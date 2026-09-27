package com.formai.api.notifications.infrastructure.persistence.repositories;

import com.formai.api.notifications.infrastructure.persistence.entities.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    // Scheduled and failed notifications are due once their time has come; the oldest go first.
    @Query(value = """
            SELECT * FROM notifications.notifications
            WHERE status IN ('SCHEDULED', 'FAILED') AND scheduled_at <= :now
            ORDER BY scheduled_at
            LIMIT :limit
            """, nativeQuery = true)
    List<NotificationJpaEntity> findAllDue(@Param("now") Instant now, @Param("limit") int limit);

    List<NotificationJpaEntity> findAllByHolderIdAndStatus(String holderId, String status);
}
