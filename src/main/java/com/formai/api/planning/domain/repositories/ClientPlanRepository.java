package com.formai.api.planning.domain.repositories;

import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.valueobjects.ClientId;

import java.util.Optional;

public interface ClientPlanRepository {

    ClientPlan save(ClientPlan plan);

    Optional<ClientPlan> findByClientIdAndHolderId(ClientId clientId, String holderId);

    Optional<ClientPlan> findByClientId(ClientId clientId);
}
