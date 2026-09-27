package com.formai.api.tracking.interfaces.rest.resources;

import java.util.List;

public record WorkoutSessionPageResource(List<WorkoutSessionResource> content,
                                         int page,
                                         int size,
                                         long totalElements,
                                         int totalPages) { }
