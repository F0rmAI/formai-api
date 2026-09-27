package com.formai.api.planning.application.internal.commandservices;

import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.exceptions.InvalidRoutineException;
import com.formai.api.planning.domain.exceptions.RoutineNotFoundException;
import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.commands.CloseRoutineCommand;
import com.formai.api.planning.domain.model.commands.CreateRoutineCommand;
import com.formai.api.planning.domain.model.commands.DuplicateRoutineCommand;
import com.formai.api.planning.domain.model.commands.MarkRoutineActiveCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;
import com.formai.api.planning.domain.model.entities.PrescribedExercise;
import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.events.RoutineUpdated;
import com.formai.api.planning.domain.repositories.ClientPlanRepository;
import com.formai.api.planning.domain.repositories.ExerciseRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import com.formai.api.planning.domain.services.RoutineCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoutineCommandServiceImpl implements RoutineCommandService {

    private final RoutineRepository routineRepository;
    private final ExerciseRepository exerciseRepository;
    private final ClientPlanRepository clientPlanRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RoutineCommandServiceImpl(RoutineRepository routineRepository,
                                     ExerciseRepository exerciseRepository,
                                     ClientPlanRepository clientPlanRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.routineRepository = routineRepository;
        this.exerciseRepository = exerciseRepository;
        this.clientPlanRepository = clientPlanRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Optional<Routine> handle(CreateRoutineCommand command) {
        var sessions = fromCatalog(command.holderId(), command.sessions());
        var routine = Routine.create(new CreateRoutineCommand(command.holderId(), command.name(), sessions));
        return Optional.of(routineRepository.save(routine));
    }

    @Override
    @Transactional
    public Optional<Routine> handle(UpdateRoutineCommand command) {
        var routine = routineRepository.findByIdAndHolderId(command.routineId(), command.holderId())
                .orElseThrow(RoutineNotFoundException::new);
        var sessions = fromCatalog(command.holderId(), command.sessions());
        var version = routine.revise(new UpdateRoutineCommand(command.routineId(), command.holderId(),
                command.name(), sessions));
        var saved = routineRepository.save(routine);

        eventPublisher.publishEvent(new RoutineUpdated(saved.getId().value(), version.getNumber()));
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public Optional<Routine> handle(DuplicateRoutineCommand command) {
        var source = routineRepository.findByIdAndHolderId(command.routineId(), command.holderId())
                .orElseThrow(RoutineNotFoundException::new);
        return Optional.of(routineRepository.save(source.duplicate(command)));
    }

    @Override
    @Transactional
    public void handle(MarkRoutineActiveCommand command) {
        routineRepository.findById(command.routineId()).ifPresent(routine -> {
            routine.markActive();
            routineRepository.save(routine);
        });
    }

    @Override
    @Transactional
    public void handle(CloseRoutineCommand command) {
        if (clientPlanRepository.existsOpenAssignmentByRoutineId(command.routineId())) {
            return;
        }
        routineRepository.findById(command.routineId()).ifPresent(routine -> {
            routine.close();
            routineRepository.save(routine);
        });
    }

    private List<RoutineSession> fromCatalog(String holderId, List<RoutineSession> sessions) {
        return sessions.stream()
                .map(session -> new RoutineSession(session.getOrder(), session.getLabel(), session.getExercises()
                        .stream()
                        .map(prescribed -> named(holderId, prescribed))
                        .toList()))
                .toList();
    }

    private PrescribedExercise named(String holderId, PrescribedExercise prescribed) {
        var exercise = exerciseRepository.findByIdAndHolderId(prescribed.getExerciseId(), holderId)
                .orElseThrow(ExerciseNotFoundException::new);
        if (!exercise.isActive()) {
            throw new InvalidRoutineException("Exercise '" + exercise.getName().value()
                    + "' is archived: restore it before using it in a routine");
        }
        return prescribed.named(exercise.getName().value());
    }
}
