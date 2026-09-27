package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.model.aggregates.Routine;

import java.util.List;

public record RoutinePage(List<Routine> items, int page, int size, long totalElements, int totalPages) { }
