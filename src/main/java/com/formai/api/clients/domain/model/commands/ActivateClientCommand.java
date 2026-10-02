package com.formai.api.clients.domain.model.commands;

import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.Email;

public record ActivateClientCommand(ClientId clientId, Email email) { }
