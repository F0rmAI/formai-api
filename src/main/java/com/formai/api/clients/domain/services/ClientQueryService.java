package com.formai.api.clients.domain.services;

import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.queries.GetClientByIdQuery;
import com.formai.api.clients.domain.model.queries.GetClientsQuery;
import com.formai.api.clients.domain.model.valueobjects.ClientPage;

import java.util.Optional;

public interface ClientQueryService {

    ClientPage handle(GetClientsQuery query);

    Optional<Client> handle(GetClientByIdQuery query);
}
