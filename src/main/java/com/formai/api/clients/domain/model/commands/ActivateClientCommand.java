package com.formai.api.clients.domain.model.commands;

import com.formai.api.clients.domain.model.valueobjects.ClientId;

public record ActivateClientCommand(ClientId clientId) { }
