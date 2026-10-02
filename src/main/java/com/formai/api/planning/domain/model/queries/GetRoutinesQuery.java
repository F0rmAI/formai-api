package com.formai.api.planning.domain.model.queries;

import com.formai.api.planning.domain.model.valueobjects.Pagination;

public record GetRoutinesQuery(String holderId, Pagination pagination) { }
