package com.formai.api.planning.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoutineResource(UUID id,
                              String name,
                              String status,
                              int currentVersion,
                              List<RoutineSessionResource> sessions,
                              Instant createdAt) { }
