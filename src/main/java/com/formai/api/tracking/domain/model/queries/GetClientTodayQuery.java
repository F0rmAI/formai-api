package com.formai.api.tracking.domain.model.queries;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;

import java.time.Instant;

public record GetClientTodayQuery(ClientId clientId, Instant now) { }
