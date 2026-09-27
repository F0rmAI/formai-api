package com.formai.api.tracking.infrastructure.persistence.repositories;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.infrastructure.persistence.transform.ActiveRoutineJpaMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ActiveRoutineRepositoryImpl implements ActiveRoutineRepository {

    private final ActiveRoutineJpaRepository jpaRepository;
    private final ActiveRoutineJpaMapper mapper;

    public ActiveRoutineRepositoryImpl(ActiveRoutineJpaRepository jpaRepository, ActiveRoutineJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ActiveRoutine save(ActiveRoutine routine) {
        var entity = mapper.toEntity(routine);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ActiveRoutine> findByClientId(ClientId clientId) {
        return jpaRepository.findByClientId(clientId.value()).map(mapper::toDomain);
    }

    @Override
    public List<ActiveRoutine> findAllByRoutineId(RoutineId routineId) {
        return jpaRepository.findAllByRoutineId(routineId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ActiveRoutine> findAllActiveOn(LocalDate date) {
        return jpaRepository.findAllActiveOn(date).stream().map(mapper::toDomain).toList();
    }
}
