package com.formai.api.clients.interfaces.rest.resources;

import java.util.List;

public record ClientPageResource(List<ClientResource> content, int page, int size, long totalElements, int totalPages) { }
