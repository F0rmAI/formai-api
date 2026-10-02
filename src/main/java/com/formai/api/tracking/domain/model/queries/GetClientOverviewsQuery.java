package com.formai.api.tracking.domain.model.queries;

import com.formai.api.tracking.domain.model.valueobjects.Pagination;

import java.util.Optional;

public record GetClientOverviewsQuery(String holderId,
                                      Optional<String> search,
                                      Optional<String> status,
                                      Pagination pagination) { }
