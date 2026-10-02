package com.formai.api.tracking.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProgressPointResource(LocalDate date, BigDecimal maxLoadKg, BigDecimal volumeKg) { }
