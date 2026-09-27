package com.formai.api.clients.domain.model.events;

import java.util.UUID;

public record ClientDeactivated(UUID clientId, String holderId) { }
