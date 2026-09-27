package com.formai.api.clients.domain.model.valueobjects;

import com.formai.api.clients.domain.model.aggregates.Client;

public record RegisteredClient(Client client, ActivationTicket ticket) { }
