package com.formai.api.notifications.domain.model.queries;

import java.time.Instant;

public record GetDueNotificationsQuery(Instant now, int limit) { }
