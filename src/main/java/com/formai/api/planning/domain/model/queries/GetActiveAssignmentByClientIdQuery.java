package com.formai.api.planning.domain.model.queries;

import com.formai.api.planning.domain.model.valueobjects.ClientId;

public record GetActiveAssignmentByClientIdQuery(ClientId clientId) { }
