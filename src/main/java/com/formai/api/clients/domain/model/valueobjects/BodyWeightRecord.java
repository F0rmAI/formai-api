package com.formai.api.clients.domain.model.valueobjects;

import java.time.LocalDate;

public record BodyWeightRecord(BodyWeight weight, LocalDate recordedOn) { }
