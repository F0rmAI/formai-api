package com.formai.api.notifications.domain.model.aggregates;

import com.formai.api.notifications.domain.model.commands.ScheduleNotificationCommand;
import com.formai.api.notifications.domain.model.valueobjects.Channel;
import com.formai.api.notifications.domain.model.valueobjects.NotificationId;
import com.formai.api.notifications.domain.model.valueobjects.NotificationStatus;
import com.formai.api.notifications.domain.model.valueobjects.NotificationType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class Notification {

    public static final int MAX_ATTEMPTS = 5;

    // The body can carry a secret (the password reset link). It is only needed until the delivery
    // is closed, so it is replaced by this text once the notification is sent, cancelled or out
    // of attempts: the token never stays in the database longer than the email is pending.
    public static final String REDACTED_BODY = "[removed after delivery]";

    private NotificationId id;
    private String holderId;
    private Channel channel;
    private NotificationType type;
    private String destination;
    private String subject;
    private String body;
    private NotificationStatus status;
    private Instant scheduledAt;
    private int attempts;
    private Instant sentAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Notification() {
    }

    public static Notification schedule(ScheduleNotificationCommand command) {
        var notification = new Notification();
        notification.id = new NotificationId(UUID.randomUUID());
        notification.holderId = command.holderId();
        notification.channel = command.channel();
        notification.type = command.type();
        notification.destination = command.destination();
        notification.subject = command.subject();
        notification.body = command.body();
        notification.status = NotificationStatus.SCHEDULED;
        notification.scheduledAt = command.scheduledAt();
        notification.attempts = 0;
        return notification;
    }

    // A failed delivery stays pending, so the dispatcher retries it, up to MAX_ATTEMPTS: after that
    // it stays FAILED for good, so a broken address or provider cannot be retried forever.
    public boolean isPending() {
        return (status == NotificationStatus.SCHEDULED || status == NotificationStatus.FAILED)
                && attempts < MAX_ATTEMPTS;
    }

    public boolean isDue(Instant now) {
        return isPending() && !scheduledAt.isAfter(now);
    }

    public void markSent(Instant at) {
        this.status = NotificationStatus.SENT;
        this.sentAt = at;
        this.attempts++;
        this.body = REDACTED_BODY;
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
        this.attempts++;
        if (!isPending()) {
            this.body = REDACTED_BODY;
        }
    }

    public void cancel() {
        this.status = NotificationStatus.CANCELLED;
        this.body = REDACTED_BODY;
    }

    public NotificationId getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public Channel getChannel() {
        return channel;
    }

    public NotificationType getType() {
        return type;
    }

    public String getDestination() {
        return destination;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public Optional<Instant> getSentAt() {
        return Optional.ofNullable(sentAt);
    }

    public void setId(NotificationId id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
