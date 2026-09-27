package com.formai.api.clients.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BodyWeightRecordResource(BigDecimal weightKg, LocalDate recordedOn) { }
