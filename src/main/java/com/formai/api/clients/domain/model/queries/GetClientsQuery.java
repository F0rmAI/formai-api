package com.formai.api.clients.domain.model.queries;

import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Pagination;

import java.util.Optional;

public record GetClientsQuery(String holderId,
                              Optional<String> search,
                              Optional<ClientStatus> status,
                              Pagination pagination) { }
