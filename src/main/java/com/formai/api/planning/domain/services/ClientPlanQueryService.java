package com.formai.api.planning.domain.services;

import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.queries.GetActiveAssignmentByClientIdQuery;
import com.formai.api.planning.domain.model.queries.GetClientPlanQuery;
import com.formai.api.planning.domain.model.valueobjects.ActiveAssignment;

import java.util.Optional;

public interface ClientPlanQueryService {

    Optional<ClientPlan> handle(GetClientPlanQuery query);

    Optional<ActiveAssignment> handle(GetActiveAssignmentByClientIdQuery query);
}
