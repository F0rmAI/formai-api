package com.formai.api.tracking.domain.model.valueobjects;

import java.util.List;

public record ClientOverviewPage(List<ClientOverview> items, int page, int size, long totalElements, int totalPages) { }
