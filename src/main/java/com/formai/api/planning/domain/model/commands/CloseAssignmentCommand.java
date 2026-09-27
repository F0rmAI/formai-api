package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.ClientId;

import java.time.LocalDate;

public record CloseAssignmentCommand(ClientId clientId, LocalDate endDate) { }
