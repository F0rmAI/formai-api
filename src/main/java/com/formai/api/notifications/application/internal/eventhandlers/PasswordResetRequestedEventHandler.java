package com.formai.api.notifications.application.internal.eventhandlers;

import com.formai.api.iam.domain.model.events.PasswordResetRequested;
import com.formai.api.notifications.domain.model.commands.ScheduleNotificationCommand;
import com.formai.api.notifications.domain.model.valueobjects.Channel;
import com.formai.api.notifications.domain.model.valueobjects.NotificationType;
import com.formai.api.notifications.domain.services.NotificationCommandService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

// Resilience: outbox + reconciliation. Sending an email is an external integration, so the handler
// only schedules the Notification (the outbox row) and PendingNotificationDispatcherJob delivers it,
// retrying failures. If this handler fails after iam's commit no email is sent; the neutral answer
// of the reset request does not change and the user can ask for a new link.
// fallbackExecution: iam publishes its events outside a transaction; without it Spring drops them.
@Component
public class PasswordResetRequestedEventHandler {

    private static final String SUBJECT = "Reset your FormAI password";

    private final NotificationCommandService notificationCommandService;
    private final String passwordResetUrl;

    public PasswordResetRequestedEventHandler(NotificationCommandService notificationCommandService,
                                              @Value("${notifications.password-reset-url}") String passwordResetUrl) {
        this.notificationCommandService = notificationCommandService;
        this.passwordResetUrl = passwordResetUrl;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PasswordResetRequested event) {
        var body = "Use this link to reset your FormAI password: " + passwordResetUrl + event.token()
                + "\nThe link works only once and expires at " + event.expiresAt() + " (UTC)."
                + "\nIf you did not ask to reset your password, ignore this email.";
        notificationCommandService.handle(new ScheduleNotificationCommand(event.holderId(), Channel.EMAIL,
                NotificationType.PASSWORD_RESET, event.email(), SUBJECT, body, Instant.now()));
    }
}
