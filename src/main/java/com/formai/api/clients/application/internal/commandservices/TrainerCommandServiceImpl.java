package com.formai.api.clients.application.internal.commandservices;

import com.formai.api.clients.domain.model.aggregates.Trainer;
import com.formai.api.clients.domain.model.commands.RegisterTrainerCommand;
import com.formai.api.clients.domain.repositories.TrainerRepository;
import com.formai.api.clients.domain.services.TrainerCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TrainerCommandServiceImpl implements TrainerCommandService {

    private final TrainerRepository trainerRepository;

    public TrainerCommandServiceImpl(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    @Override
    @Transactional
    public Optional<Trainer> handle(RegisterTrainerCommand command) {
        if (trainerRepository.existsByHolderId(command.holderId())) {
            return trainerRepository.findByHolderId(command.holderId());
        }
        return Optional.of(trainerRepository.save(Trainer.register(command)));
    }
}
