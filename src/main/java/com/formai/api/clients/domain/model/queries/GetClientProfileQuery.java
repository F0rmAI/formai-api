package com.formai.api.clients.domain.model.queries;

import com.formai.api.clients.domain.model.valueobjects.ClientId;

// The signed-in client reading their own record: no trainer involved, unlike GetClientByIdQuery.
public record GetClientProfileQuery(ClientId clientId) { }
