package com.formai.api.planning.domain.model.valueobjects;

import com.formai.api.planning.domain.model.aggregates.Exercise;

import java.util.List;

public record ExercisePage(List<Exercise> items, int page, int size, long totalElements, int totalPages) { }
