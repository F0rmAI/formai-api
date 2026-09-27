package com.formai.api.shared.contracts.clients;

import java.util.List;

public record ClientSummaryPage(List<ClientSummary> items, int page, int size, long totalElements, int totalPages) { }
