package com.formai.api.planning.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;

public record RoutineVersionResource(int number, Instant changedAt, String author,
                                     List<RoutineSessionResource> sessions) { }
