package com.formai.api.clients.domain.repositories;

import com.formai.api.clients.domain.model.aggregates.Trainer;

import java.util.Optional;

public interface TrainerRepository {

    Trainer save(Trainer trainer);

    Optional<Trainer> findByHolderId(String holderId);

    boolean existsByHolderId(String holderId);
}
