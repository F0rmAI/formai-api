package com.formai.api.planning.application.internal.commandservices;

import com.formai.api.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import com.formai.api.planning.domain.exceptions.ExerciseAlreadyExistsException;
import com.formai.api.planning.domain.exceptions.ExerciseInUseException;
import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.exceptions.MachineNotPublishedException;
import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.commands.ArchiveExerciseCommand;
import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.commands.DeleteExerciseCommand;
import com.formai.api.planning.domain.model.commands.LinkExerciseToMachineCommand;
import com.formai.api.planning.domain.model.commands.RestoreExerciseCommand;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.repositories.ExerciseRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import com.formai.api.planning.domain.services.ExerciseCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ExerciseCommandServiceImpl implements ExerciseCommandService {

    private final ExerciseRepository exerciseRepository;
    private final RoutineRepository routineRepository;
    private final ExternalCatalogService externalCatalogService;

    public ExerciseCommandServiceImpl(ExerciseRepository exerciseRepository, RoutineRepository routineRepository,
                                      ExternalCatalogService externalCatalogService) {
        this.exerciseRepository = exerciseRepository;
        this.routineRepository = routineRepository;
        this.externalCatalogService = externalCatalogService;
    }

    @Override
    @Transactional
    public Optional<Exercise> handle(CreateExerciseCommand command) {
        if (exerciseRepository.existsByHolderIdAndName(command.holderId(), command.name())) {
            throw new ExerciseAlreadyExistsException(command.name().value());
        }
        return Optional.of(exerciseRepository.save(Exercise.create(command)));
    }

    @Override
    @Transactional
    public Optional<Exercise> handle(ArchiveExerciseCommand command) {
        var exercise = findOwnExercise(command.exerciseId(), command.holderId());
        exercise.archive();
        return Optional.of(exerciseRepository.save(exercise));
    }

    @Override
    @Transactional
    public Optional<Exercise> handle(RestoreExerciseCommand command) {
        var exercise = findOwnExercise(command.exerciseId(), command.holderId());
        exercise.restore();
        return Optional.of(exerciseRepository.save(exercise));
    }

    @Override
    @Transactional
    public void handle(DeleteExerciseCommand command) {
        var exercise = findOwnExercise(command.exerciseId(), command.holderId());
        if (routineRepository.existsByHolderIdAndExerciseId(command.holderId(), command.exerciseId())) {
            throw new ExerciseInUseException();
        }
        exerciseRepository.delete(exercise);
    }

    @Override
    @Transactional
    public Optional<Exercise> handle(LinkExerciseToMachineCommand command) {
        var exercise = findOwnExercise(command.exerciseId(), command.holderId());
        if (!externalCatalogService.isMachinePublished(command.machineId())) {
            throw new MachineNotPublishedException();
        }
        exercise.linkToMachine(command);
        return Optional.of(exerciseRepository.save(exercise));
    }

    private Exercise findOwnExercise(ExerciseId exerciseId, String holderId) {
        return exerciseRepository.findByIdAndHolderId(exerciseId, holderId)
                .orElseThrow(ExerciseNotFoundException::new);
    }
}
