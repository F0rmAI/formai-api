package com.formai.api.planning.application.internal.queryservices;

import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.entities.RoutineVersion;
import com.formai.api.planning.domain.model.queries.GetRoutineByIdQuery;
import com.formai.api.planning.domain.model.queries.GetRoutineVersionsQuery;
import com.formai.api.planning.domain.model.queries.GetRoutinesQuery;
import com.formai.api.planning.domain.model.valueobjects.RoutinePage;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import com.formai.api.planning.domain.services.RoutineQueryService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RoutineQueryServiceImpl implements RoutineQueryService {

    private final RoutineRepository routineRepository;

    public RoutineQueryServiceImpl(RoutineRepository routineRepository) {
        this.routineRepository = routineRepository;
    }

    @Override
    public RoutinePage handle(GetRoutinesQuery query) {
        return routineRepository.findAllByHolderId(query.holderId(), query.pagination());
    }

    @Override
    public Optional<Routine> handle(GetRoutineByIdQuery query) {
        return routineRepository.findByIdAndHolderId(query.routineId(), query.holderId());
    }

    @Override
    public List<RoutineVersion> handle(GetRoutineVersionsQuery query) {
        return routineRepository.findByIdAndHolderId(query.routineId(), query.holderId())
                .map(routine -> routine.getVersions().stream()
                        .sorted(Comparator.comparingInt(RoutineVersion::getNumber).reversed())
                        .toList())
                .orElse(List.of());
    }
}
