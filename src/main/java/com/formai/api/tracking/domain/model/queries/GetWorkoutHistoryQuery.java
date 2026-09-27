package com.formai.api.tracking.domain.model.queries;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;

import java.util.Optional;

public record GetWorkoutHistoryQuery(ClientId clientId,
                                     String requesterHolderId,
                                     Optional<ReportPeriod> period,
                                     Pagination pagination) { }
