package com.formai.api.clients.domain.model.commands;

import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.FullName;

public record UpdateClientCommand(ClientId clientId, String holderId, FullName fullName) { }
