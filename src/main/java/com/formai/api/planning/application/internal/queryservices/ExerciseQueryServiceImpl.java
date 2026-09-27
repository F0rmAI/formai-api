package com.formai.api.planning.application.internal.queryservices;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.queries.GetExerciseByIdQuery;
import com.formai.api.planning.domain.model.queries.GetExercisesQuery;
import com.formai.api.planning.domain.model.valueobjects.ExercisePage;
import com.formai.api.planning.domain.repositories.ExerciseRepository;
import com.formai.api.planning.domain.services.ExerciseQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExerciseQueryServiceImpl implements ExerciseQueryService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseQueryServiceImpl(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    public ExercisePage handle(GetExercisesQuery query) {
        return exerciseRepository.findAllByHolderId(query.holderId(), query.search(), query.status(),
                query.pagination());
    }

    @Override
    public Optional<Exercise> handle(GetExerciseByIdQuery query) {
        return exerciseRepository.findByIdAndHolderId(query.exerciseId(), query.holderId());
    }
}
