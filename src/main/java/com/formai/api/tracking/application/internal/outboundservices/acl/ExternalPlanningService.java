package com.formai.api.tracking.application.internal.outboundservices.acl;

import com.formai.api.planning.interfaces.acl.PlanningContextFacade;
import com.formai.api.shared.contracts.planning.ActiveRoutineSnapshot;
import com.formai.api.shared.contracts.planning.PrescribedExerciseSnapshot;
import com.formai.api.shared.contracts.planning.SessionSnapshot;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseToPerform;
import com.formai.api.tracking.domain.model.valueobjects.PlannedRoutine;
import com.formai.api.tracking.domain.model.valueobjects.RoutineDay;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import org.springframework.stereotype.Service;

import java.util.Optional;

// ACL over planning's Open Host Service: translates its published snapshot into
// tracking's own PlannedRoutine, so planning's types never reach tracking's domain.
@Service
public class ExternalPlanningService {

    private final PlanningContextFacade planningContextFacade;

    public ExternalPlanningService(PlanningContextFacade planningContextFacade) {
        this.planningContextFacade = planningContextFacade;
    }

    public Optional<PlannedRoutine> fetchActiveRoutine(ClientId clientId) {
        return planningContextFacade.fetchActiveRoutine(clientId.value()).map(this::toPlannedRoutine);
    }

    private PlannedRoutine toPlannedRoutine(ActiveRoutineSnapshot snapshot) {
        return new PlannedRoutine(
                new RoutineId(snapshot.routineId()),
                snapshot.routineName(),
                snapshot.version(),
                snapshot.startDate(),
                snapshot.sessions().stream().map(this::toRoutineDay).toList());
    }

    private RoutineDay toRoutineDay(SessionSnapshot session) {
        return new RoutineDay(session.order(), session.label(),
                session.exercises().stream().map(this::toExerciseToPerform).toList());
    }

    private ExerciseToPerform toExerciseToPerform(PrescribedExerciseSnapshot exercise) {
        return new ExerciseToPerform(
                new ExerciseId(exercise.exerciseId()),
                exercise.exerciseName(),
                exercise.sets(),
                exercise.reps(),
                exercise.targetLoadKg(),
                exercise.restSeconds());
    }
}
