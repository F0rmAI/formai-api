package com.formai.api.planning.domain.model.commands;

import com.formai.api.planning.domain.model.valueobjects.ClientId;

import java.time.LocalDate;

public record TransferClientPlanCommand(ClientId clientId, String newHolderId, LocalDate date) { }
