package com.formai.api.clients.domain.model.events;

import java.util.UUID;

public record ClientTransferred(UUID clientId, String previousHolderId, String newHolderId) { }
