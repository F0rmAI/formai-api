package com.formai.api.tracking.application.internal.queryservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalClientsService;
import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.queries.GetWorkoutHistoryQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutSessionByIdQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import com.formai.api.tracking.domain.services.WorkoutSessionQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WorkoutSessionQueryServiceImpl implements WorkoutSessionQueryService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final ExternalClientsService externalClientsService;

    public WorkoutSessionQueryServiceImpl(WorkoutSessionRepository workoutSessionRepository,
                                          ExternalClientsService externalClientsService) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.externalClientsService = externalClientsService;
    }

    @Override
    public Optional<WorkoutSession> handle(GetWorkoutSessionByIdQuery query) {
        ensureCanRead(query.clientId(), query.requesterHolderId());
        return workoutSessionRepository.findByIdAndClientId(query.workoutSessionId(), query.clientId());
    }

    @Override
    public WorkoutSessionPage handle(GetWorkoutHistoryQuery query) {
        ensureCanRead(query.clientId(), query.requesterHolderId());
        return workoutSessionRepository.findAllByClientId(query.clientId(), query.period(), query.pagination());
    }

    // A client reads their own workouts; anyone else must be that client's trainer (IDOR).
    private void ensureCanRead(ClientId clientId, String requesterHolderId) {
        var isOwnHistory = clientId.value().toString().equals(requesterHolderId);
        if (!isOwnHistory && !externalClientsService.isClientOfTrainer(clientId, requesterHolderId)) {
            throw new ClientAccessDeniedException();
        }
    }
}
