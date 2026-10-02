package com.formai.api.planning.interfaces.acl;

import com.formai.api.shared.contracts.planning.ActiveRoutineSnapshot;

import java.util.Optional;
import java.util.UUID;

public interface PlanningContextFacade {

    Optional<ActiveRoutineSnapshot> fetchActiveRoutine(UUID clientId);
}
