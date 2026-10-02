package com.formai.api.tracking.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record ProgressChartResource(UUID exerciseId, int weeks, boolean enoughData, List<ProgressPointResource> points) { }
