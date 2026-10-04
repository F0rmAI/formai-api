package com.formai.api.tracking.domain.model.commands;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;

import java.time.LocalDate;

// Closes the client's pending sessions scheduled before the given date (the client's today).
public record CloseClientOverdueWorkoutSessionsCommand(ClientId clientId, LocalDate date) { }
