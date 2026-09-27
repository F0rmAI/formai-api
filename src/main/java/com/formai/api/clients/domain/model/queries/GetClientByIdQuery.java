package com.formai.api.clients.domain.model.queries;

import com.formai.api.clients.domain.model.valueobjects.ClientId;

public record GetClientByIdQuery(ClientId clientId, String holderId) { }
