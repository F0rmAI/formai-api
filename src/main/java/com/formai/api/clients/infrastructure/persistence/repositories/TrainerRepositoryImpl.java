package com.formai.api.clients.infrastructure.persistence.repositories;

import com.formai.api.clients.domain.model.aggregates.Trainer;
import com.formai.api.clients.domain.repositories.TrainerRepository;
import com.formai.api.clients.infrastructure.persistence.transform.TrainerJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TrainerRepositoryImpl implements TrainerRepository {

    private final TrainerJpaRepository jpaRepository;
    private final TrainerJpaMapper mapper;

    public TrainerRepositoryImpl(TrainerJpaRepository jpaRepository, TrainerJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Trainer save(Trainer trainer) {
        var entity = mapper.toEntity(trainer);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Trainer> findByHolderId(String holderId) {
        return jpaRepository.findByHolderId(holderId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByHolderId(String holderId) {
        return jpaRepository.existsByHolderId(holderId);
    }
}
