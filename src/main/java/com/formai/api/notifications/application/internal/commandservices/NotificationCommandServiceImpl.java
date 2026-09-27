package com.formai.api.notifications.application.internal.commandservices;

import com.formai.api.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import com.formai.api.notifications.domain.model.aggregates.Notification;
import com.formai.api.notifications.domain.model.commands.ScheduleNotificationCommand;
import com.formai.api.notifications.domain.model.commands.SendNotificationCommand;
import com.formai.api.notifications.domain.model.valueobjects.NotificationStatus;
import com.formai.api.notifications.domain.repositories.NotificationRepository;
import com.formai.api.notifications.domain.services.NotificationCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCommandServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final EmailDeliveryService emailDeliveryService;

    public NotificationCommandServiceImpl(NotificationRepository notificationRepository,
                                          EmailDeliveryService emailDeliveryService) {
        this.notificationRepository = notificationRepository;
        this.emailDeliveryService = emailDeliveryService;
    }

    // A newer notification of the same type replaces the ones still waiting: a new password reset
    // link invalidates the previous one, so the old email must not go out.
    @Override
    @Transactional
    public Optional<Notification> handle(ScheduleNotificationCommand command) {
        notificationRepository.findAllByHolderIdAndStatus(command.holderId(), NotificationStatus.SCHEDULED).stream()
                .filter(pending -> pending.getType() == command.type())
                .forEach(pending -> {
                    pending.cancel();
                    notificationRepository.save(pending);
                });
        return Optional.of(notificationRepository.save(Notification.schedule(command)));
    }

    @Override
    @Transactional
    public void handle(SendNotificationCommand command) {
        var now = Instant.now();
        var notification = notificationRepository.findById(command.notificationId())
                .filter(found -> found.isDue(now));
        if (notification.isEmpty()) {
            return;
        }
        var pending = notification.get();
        boolean delivered;
        try {
            delivered = emailDeliveryService.send(pending.getDestination(), pending.getSubject(), pending.getBody());
        } catch (RuntimeException ex) {
            log.warn("Could not deliver notification {}", pending.getId().value(), ex);
            delivered = false;
        }
        if (delivered) {
            pending.markSent(now);
        } else {
            pending.markFailed();
        }
        notificationRepository.save(pending);
    }
}
