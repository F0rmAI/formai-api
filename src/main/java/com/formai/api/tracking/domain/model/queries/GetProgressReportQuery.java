package com.formai.api.tracking.domain.model.queries;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;

public record GetProgressReportQuery(ClientId clientId, String requesterHolderId, ReportPeriod period) { }
