package com.formai.api.clients.domain.services;

import com.formai.api.clients.domain.model.aggregates.Trainer;
import com.formai.api.clients.domain.model.commands.RegisterTrainerCommand;

import java.util.Optional;

public interface TrainerCommandService {

    Optional<Trainer> handle(RegisterTrainerCommand command);
}
