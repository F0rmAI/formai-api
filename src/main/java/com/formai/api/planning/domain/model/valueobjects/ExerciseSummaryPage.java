package com.formai.api.planning.domain.model.valueobjects;

import java.util.List;

public record ExerciseSummaryPage(List<ExerciseSummary> items, int page, int size, long totalElements,
                                  int totalPages) { }
