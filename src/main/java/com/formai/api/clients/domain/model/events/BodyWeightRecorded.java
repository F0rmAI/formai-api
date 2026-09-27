package com.formai.api.clients.domain.model.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BodyWeightRecorded(UUID clientId, BigDecimal kilograms, LocalDate recordedOn) { }
