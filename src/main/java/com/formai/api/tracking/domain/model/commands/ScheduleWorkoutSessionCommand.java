package com.formai.api.tracking.domain.model.commands;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;

import java.time.LocalDate;

public record ScheduleWorkoutSessionCommand(ClientId clientId, LocalDate date) { }
