package com.formai.api.clients.domain.model.events;

import java.util.UUID;

public record ClientRegistered(UUID clientId, String holderId) { }
