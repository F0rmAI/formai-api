package com.formai.api.tracking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.Instant;

public record SetEntryResource(int setNumber, BigDecimal loadKg, int reps, Instant recordedAt) { }
