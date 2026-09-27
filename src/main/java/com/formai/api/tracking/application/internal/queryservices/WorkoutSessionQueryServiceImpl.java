package com.formai.api.tracking.application.internal.queryservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalClientsService;
import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.queries.GetClientOverviewsQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutHistoryQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutSessionByIdQuery;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ClientOverview;
import com.formai.api.tracking.domain.model.valueobjects.ClientOverviewPage;
import com.formai.api.tracking.domain.model.valueobjects.LastWorkout;
import com.formai.api.tracking.domain.model.valueobjects.TrainerClient;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import com.formai.api.tracking.domain.services.WorkoutSessionQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WorkoutSessionQueryServiceImpl implements WorkoutSessionQueryService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final ActiveRoutineRepository activeRoutineRepository;
    private final ExternalClientsService externalClientsService;

    public WorkoutSessionQueryServiceImpl(WorkoutSessionRepository workoutSessionRepository,
                                          ActiveRoutineRepository activeRoutineRepository,
                                          ExternalClientsService externalClientsService) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.activeRoutineRepository = activeRoutineRepository;
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

    // The trainer's client list (FR-006): clients gives the page of clients, already scoped
    // to the trainer; tracking adds each one's current routine and last workout date.
    @Override
    public ClientOverviewPage handle(GetClientOverviewsQuery query) {
        var clients = externalClientsService.fetchClientsOfTrainer(query.holderId(), query.search(), query.status(),
                query.pagination());
        var clientIds = clients.items().stream().map(TrainerClient::clientId).toList();
        if (clientIds.isEmpty()) {
            return new ClientOverviewPage(List.of(), clients.page(), clients.size(), clients.totalElements(),
                    clients.totalPages());
        }
        var today = LocalDate.now();
        Map<ClientId, String> routineNames = activeRoutineRepository.findAllByClientIds(clientIds).stream()
                .filter(routine -> routine.isActiveOn(today))
                .collect(Collectors.toMap(ActiveRoutine::getClientId, ActiveRoutine::getRoutineName));
        Map<ClientId, LocalDate> lastWorkouts = workoutSessionRepository.findLastWorkoutDates(clientIds).stream()
                .collect(Collectors.toMap(LastWorkout::clientId, LastWorkout::lastWorkoutOn));
        var items = clients.items().stream()
                .map(client -> new ClientOverview(client.clientId(), client.fullName(), client.status(),
                        Optional.ofNullable(routineNames.get(client.clientId())),
                        Optional.ofNullable(lastWorkouts.get(client.clientId()))))
                .toList();
        return new ClientOverviewPage(items, clients.page(), clients.size(), clients.totalElements(),
                clients.totalPages());
    }

    // A client reads their own workouts; anyone else must be that client's trainer (IDOR).
    private void ensureCanRead(ClientId clientId, String requesterHolderId) {
        var isOwnHistory = clientId.value().toString().equals(requesterHolderId);
        if (!isOwnHistory && !externalClientsService.isClientOfTrainer(clientId, requesterHolderId)) {
            throw new ClientAccessDeniedException();
        }
    }
}
