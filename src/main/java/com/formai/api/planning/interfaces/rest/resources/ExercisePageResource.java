package com.formai.api.planning.interfaces.rest.resources;

import java.util.List;

public record ExercisePageResource(List<ExerciseResource> content, int page, int size, long totalElements, int totalPages) { }
