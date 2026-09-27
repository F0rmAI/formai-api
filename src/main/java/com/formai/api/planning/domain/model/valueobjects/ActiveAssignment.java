package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.model.aggregates.Routine;

import java.time.LocalDate;

public record ActiveAssignment(ClientId clientId, Routine routine, LocalDate startDate) { }
