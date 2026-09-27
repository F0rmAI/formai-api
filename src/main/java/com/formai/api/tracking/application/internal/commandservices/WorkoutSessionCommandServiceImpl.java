package com.formai.api.tracking.application.internal.commandservices;

import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionNotFoundException;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.commands.CorrectSetCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.SkipOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.events.SetRecorded;
import com.formai.api.tracking.domain.model.events.WorkoutSessionFinished;
import com.formai.api.tracking.domain.model.events.WorkoutSessionScheduled;
import com.formai.api.tracking.domain.model.events.WorkoutSessionSkipped;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WorkoutSessionCommandServiceImpl implements WorkoutSessionCommandService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final ActiveRoutineRepository activeRoutineRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WorkoutSessionCommandServiceImpl(WorkoutSessionRepository workoutSessionRepository,
                                            ActiveRoutineRepository activeRoutineRepository,
                                            ApplicationEventPublisher eventPublisher) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.activeRoutineRepository = activeRoutineRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<WorkoutSession> handle(ScheduleWorkoutSessionCommand command) {
        var existing = workoutSessionRepository.findByClientIdAndScheduledFor(command.clientId(), command.date());
        if (existing.isPresent()) {
            return existing;
        }
        var routine = activeRoutineRepository.findByClientId(command.clientId())
                .filter(candidate -> candidate.isActiveOn(command.date()))
                .orElseThrow(ActiveRoutineNotFoundException::new);
        var lastOrder = workoutSessionRepository.findLastFinishedByClientId(command.clientId())
                .filter(last -> last.getRoutineId().equals(routine.getRoutineId()))
                .map(WorkoutSession::getDayOrder);
        var session = WorkoutSession.schedule(command, routine, routine.nextDay(lastOrder));
        var saved = workoutSessionRepository.save(session);

        eventPublisher.publishEvent(new WorkoutSessionScheduled(saved.getId().value(), saved.getClientId().value(),
                saved.getScheduledFor(), saved.getDayLabel()));
        return Optional.of(saved);
    }

    @Override
    public Optional<WorkoutSession> handle(RecordSetCommand command) {
        var session = findOwnSession(command.workoutSessionId(), command.clientId());
        session.recordSet(command);
        var saved = workoutSessionRepository.save(session);

        eventPublisher.publishEvent(new SetRecorded(saved.getId().value(), command.exerciseId().value(),
                command.setNumber()));
        return Optional.of(saved);
    }

    @Override
    public Optional<WorkoutSession> handle(CorrectSetCommand command) {
        var session = findOwnSession(command.workoutSessionId(), command.clientId());
        session.correctSet(command);
        return Optional.of(workoutSessionRepository.save(session));
    }

    @Override
    public Optional<WorkoutSession> handle(FinishWorkoutSessionCommand command) {
        var session = findOwnSession(command.workoutSessionId(), command.clientId());
        var status = session.finish(command);
        var saved = workoutSessionRepository.save(session);

        eventPublisher.publishEvent(new WorkoutSessionFinished(saved.getId().value(), saved.getClientId().value(),
                status.name()));
        return Optional.of(saved);
    }

    @Override
    public void handle(SkipOverdueWorkoutSessionsCommand command) {
        workoutSessionRepository.findAllPendingBefore(command.date()).stream()
                .filter(session -> !session.hasRecords())
                .forEach(session -> {
                    session.skip();
                    var saved = workoutSessionRepository.save(session);
                    eventPublisher.publishEvent(new WorkoutSessionSkipped(saved.getId().value(),
                            saved.getClientId().value()));
                });
    }

    private WorkoutSession findOwnSession(WorkoutSessionId id, ClientId clientId) {
        return workoutSessionRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(WorkoutSessionNotFoundException::new);
    }
}
