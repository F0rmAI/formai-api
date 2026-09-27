package com.formai.api.tracking.domain.model.commands;

import java.time.LocalDate;

public record SkipOverdueWorkoutSessionsCommand(LocalDate date) { }
