package com.formai.api.tracking.domain.model.queries;

import java.time.Instant;

// The routines active at that instant, each on its client's own calendar date.
public record GetActiveRoutinesAtQuery(Instant now) { }
