package com.formai.api.tracking.interfaces.rest.resources;

import java.util.List;

public record ClientOverviewPageResource(List<ClientOverviewResource> content,
                                         int page,
                                         int size,
                                         long totalElements,
                                         int totalPages) { }
