package com.formai.api.tracking.domain.model.valueobjects;

import java.time.LocalDate;

public record ProgressPoint(LocalDate date, Load maxLoad, TrainingVolume volume) { }
