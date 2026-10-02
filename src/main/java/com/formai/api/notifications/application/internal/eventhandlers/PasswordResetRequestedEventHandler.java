package com.formai.api.notifications.application.internal.eventhandlers;

import com.formai.api.iam.domain.model.events.PasswordResetRequested;
import com.formai.api.notifications.domain.model.commands.ScheduleNotificationCommand;
import com.formai.api.notifications.domain.model.valueobjects.Channel;
import com.formai.api.notifications.domain.model.valueobjects.NotificationType;
import com.formai.api.notifications.domain.services.NotificationCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

// Resilience: outbox + reconciliation. Sending an email is an external integration, so the handler
// only schedules the Notification (the outbox row) and PendingNotificationDispatcherJob delivers it,
// retrying failures. If this handler fails after iam's commit no email is sent; the neutral answer
// of the reset request does not change and the user can ask for a new link.
// fallbackExecution: iam publishes its events outside a transaction; without it Spring drops them.
@Component
public class PasswordResetRequestedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetRequestedEventHandler.class);
    private static final String SUBJECT = "Reset your FormAI password";

    private final NotificationCommandService notificationCommandService;
    private final TransactionTemplate newTransaction;
    private final String passwordResetUrl;

    public PasswordResetRequestedEventHandler(NotificationCommandService notificationCommandService,
                                              PlatformTransactionManager transactionManager,
                                              @Value("${notifications.password-reset-url}") String passwordResetUrl) {
        this.notificationCommandService = notificationCommandService;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.passwordResetUrl = passwordResetUrl;
    }

    // REQUIRES_NEW through TransactionTemplate instead of @Transactional: the failure has to be
    // caught outside the transaction, or its rollback would surface as UnexpectedRollbackException.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PasswordResetRequested event) {
        var body = "Use this link to reset your FormAI password: " + passwordResetUrl + event.token()
                + "\nThe link works only once and expires at " + event.expiresAt() + " (UTC)."
                + "\nIf you did not ask to reset your password, ignore this email.";
        // iam publishes without a transaction, so this listener runs inside the reset request: a
        // failure here must not reach it, or the request would answer 500 only for existing emails.
        // Never log the exception itself: its message can carry the address or the body.
        var command = new ScheduleNotificationCommand(event.holderId(), Channel.EMAIL,
                NotificationType.PASSWORD_RESET, event.email(), SUBJECT, body, Instant.now());
        try {
            newTransaction.executeWithoutResult(status -> notificationCommandService.handle(command));
        } catch (RuntimeException ex) {
            log.error("Could not schedule the password reset email ({})", ex.getClass().getSimpleName());
        }
    }
}
