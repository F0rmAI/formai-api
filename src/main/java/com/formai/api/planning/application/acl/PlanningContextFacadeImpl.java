package com.formai.api.planning.application.acl;

import com.formai.api.planning.domain.model.entities.PrescribedExercise;
import com.formai.api.planning.domain.model.entities.RoutineSession;
import com.formai.api.planning.domain.model.queries.GetActiveAssignmentByClientIdQuery;
import com.formai.api.planning.domain.model.valueobjects.ActiveAssignment;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.services.ClientPlanQueryService;
import com.formai.api.planning.interfaces.acl.PlanningContextFacade;
import com.formai.api.shared.contracts.planning.ActiveRoutineSnapshot;
import com.formai.api.shared.contracts.planning.PrescribedExerciseSnapshot;
import com.formai.api.shared.contracts.planning.SessionSnapshot;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlanningContextFacadeImpl implements PlanningContextFacade {

    private final ClientPlanQueryService clientPlanQueryService;

    public PlanningContextFacadeImpl(ClientPlanQueryService clientPlanQueryService) {
        this.clientPlanQueryService = clientPlanQueryService;
    }

    @Override
    public Optional<ActiveRoutineSnapshot> fetchActiveRoutine(UUID clientId) {
        return clientPlanQueryService.handle(new GetActiveAssignmentByClientIdQuery(new ClientId(clientId)))
                .map(this::toSnapshot);
    }

    private ActiveRoutineSnapshot toSnapshot(ActiveAssignment assignment) {
        var routine = assignment.routine();
        var version = routine.currentVersion();
        return new ActiveRoutineSnapshot(
                assignment.clientId().value(),
                routine.getId().value(),
                routine.getName().value(),
                version.getNumber(),
                assignment.startDate(),
                new LinkedHashSet<>(assignment.trainingDays().names()),
                version.getSessions().stream().map(this::toSnapshot).toList());
    }

    private SessionSnapshot toSnapshot(RoutineSession session) {
        return new SessionSnapshot(session.getOrder(), session.getLabel(),
                session.getExercises().stream().map(this::toSnapshot).toList());
    }

    private PrescribedExerciseSnapshot toSnapshot(PrescribedExercise exercise) {
        var prescription = exercise.getPrescription();
        return new PrescribedExerciseSnapshot(exercise.getExerciseId().value(), exercise.getExerciseName(),
                prescription.sets(), prescription.reps(), prescription.targetLoadKg(), prescription.restSeconds());
    }
}
