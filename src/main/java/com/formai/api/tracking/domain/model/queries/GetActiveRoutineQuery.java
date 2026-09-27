package com.formai.api.tracking.domain.model.queries;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;

import java.time.LocalDate;

public record GetActiveRoutineQuery(ClientId clientId, LocalDate today) { }
