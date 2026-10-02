package com.formai.api.clients.domain.model.valueobjects;

import com.formai.api.clients.domain.model.aggregates.Client;

import java.util.List;

public record ClientPage(List<Client> items, int page, int size, long totalElements, int totalPages) { }
