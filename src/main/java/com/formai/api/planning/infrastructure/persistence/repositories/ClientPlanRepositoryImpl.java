package com.formai.api.planning.infrastructure.persistence.repositories;

import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.repositories.ClientPlanRepository;
import com.formai.api.planning.infrastructure.persistence.transform.ClientPlanJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ClientPlanRepositoryImpl implements ClientPlanRepository {

    private final ClientPlanJpaRepository jpaRepository;
    private final ClientPlanJpaMapper mapper;

    public ClientPlanRepositoryImpl(ClientPlanJpaRepository jpaRepository, ClientPlanJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ClientPlan save(ClientPlan plan) {
        var entity = mapper.toEntity(plan);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ClientPlan> findByClientIdAndHolderId(ClientId clientId, String holderId) {
        return jpaRepository.findByClientIdAndHolderId(clientId.value(), holderId).map(mapper::toDomain);
    }

    @Override
    public Optional<ClientPlan> findByClientId(ClientId clientId) {
        return jpaRepository.findByClientId(clientId.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsOpenAssignmentByRoutineId(RoutineId routineId) {
        return jpaRepository.existsOpenAssignmentByRoutineId(routineId.value());
    }
}
