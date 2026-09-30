package com.formai.api.planning.application.internal.queryservices;

import com.formai.api.planning.application.internal.outboundservices.acl.ExternalClientsService;
import com.formai.api.planning.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.queries.GetActiveAssignmentByClientIdQuery;
import com.formai.api.planning.domain.model.queries.GetClientPlanQuery;
import com.formai.api.planning.domain.model.valueobjects.ActiveAssignment;
import com.formai.api.planning.domain.repositories.ClientPlanRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import com.formai.api.planning.domain.services.ClientPlanQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ClientPlanQueryServiceImpl implements ClientPlanQueryService {

    private final ClientPlanRepository clientPlanRepository;
    private final RoutineRepository routineRepository;
    private final ExternalClientsService externalClientsService;

    public ClientPlanQueryServiceImpl(ClientPlanRepository clientPlanRepository, RoutineRepository routineRepository,
                                      ExternalClientsService externalClientsService) {
        this.clientPlanRepository = clientPlanRepository;
        this.routineRepository = routineRepository;
        this.externalClientsService = externalClientsService;
    }

    // Another trainer's client answers 403 (FR-016), even through a direct URL; one of the
    // trainer's own clients, active or not, keeps its assignment history.
    @Override
    public Optional<ClientPlan> handle(GetClientPlanQuery query) {
        if (externalClientsService.isActiveClientOfTrainer(query.clientId(), query.holderId()).isEmpty()) {
            throw new ClientAccessDeniedException();
        }
        return clientPlanRepository.findByClientIdAndHolderId(query.clientId(), query.holderId());
    }

    @Override
    public Optional<ActiveAssignment> handle(GetActiveAssignmentByClientIdQuery query) {
        var today = LocalDate.now();
        return clientPlanRepository.findByClientId(query.clientId())
                .flatMap(plan -> plan.getAssignments().stream()
                        .filter(assignment -> isInEffectOn(assignment, today))
                        .findFirst()
                        .or(plan::currentAssignment))
                .flatMap(assignment -> routineRepository.findById(assignment.getRoutineId())
                        .map(routine -> new ActiveAssignment(query.clientId(), routine,
                                assignment.getPeriod().startDate())));
    }

    private static boolean isInEffectOn(Assignment assignment, LocalDate date) {
        var period = assignment.getPeriod();
        return !period.startDate().isAfter(date) && (period.endDate() == null || !period.endDate().isBefore(date));
    }
}
