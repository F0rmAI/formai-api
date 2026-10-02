-- Outbox of the notifications to deliver: PendingNotificationDispatcherJob sends the due ones.
CREATE TABLE IF NOT EXISTS notifications.notifications (
    id UUID PRIMARY KEY,
    holder_id VARCHAR(64) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    type VARCHAR(40) NOT NULL,
    destination VARCHAR(254) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_status_scheduled_at
    ON notifications.notifications (status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_notifications_holder_status
    ON notifications.notifications (holder_id, status);
