package com.formai.api.planning.application.internal.queryservices;

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

    public ClientPlanQueryServiceImpl(ClientPlanRepository clientPlanRepository, RoutineRepository routineRepository) {
        this.clientPlanRepository = clientPlanRepository;
        this.routineRepository = routineRepository;
    }

    @Override
    public Optional<ClientPlan> handle(GetClientPlanQuery query) {
        return clientPlanRepository.findByClientIdAndHolderId(query.clientId(), query.holderId());
    }

    // The assignment in effect today. A new assignment with a later start date only closes
    // the previous one from the day before, so until then the previous one is still served;
    // with none in effect, the open (upcoming) one is.
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
