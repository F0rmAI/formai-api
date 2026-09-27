package com.formai.api.tracking.domain.model.commands;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;

public record SyncActiveRoutineCommand(ClientId clientId) { }
