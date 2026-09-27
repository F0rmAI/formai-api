package com.formai.api.tracking.domain.repositories;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActiveRoutineRepository {

    ActiveRoutine save(ActiveRoutine routine);

    Optional<ActiveRoutine> findByClientId(ClientId clientId);

    List<ActiveRoutine> findAllByRoutineId(RoutineId routineId);

    List<ActiveRoutine> findAllActiveOn(LocalDate date);

    List<ActiveRoutine> findAllByClientIds(List<ClientId> clientIds);
}
