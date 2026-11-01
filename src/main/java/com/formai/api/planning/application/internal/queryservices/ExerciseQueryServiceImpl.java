package com.formai.api.planning.application.internal.queryservices;

import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.queries.GetExerciseByIdQuery;
import com.formai.api.planning.domain.model.queries.GetExercisesQuery;
import com.formai.api.planning.domain.model.queries.GetMuscleGroupsQuery;
import com.formai.api.planning.domain.model.valueobjects.ExerciseSummary;
import com.formai.api.planning.domain.model.valueobjects.ExerciseSummaryPage;
import com.formai.api.planning.domain.model.valueobjects.MuscleGroup;
import com.formai.api.planning.domain.repositories.ExerciseRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import com.formai.api.planning.domain.services.ExerciseQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExerciseQueryServiceImpl implements ExerciseQueryService {

    private final ExerciseRepository exerciseRepository;
    private final RoutineRepository routineRepository;

    public ExerciseQueryServiceImpl(ExerciseRepository exerciseRepository, RoutineRepository routineRepository) {
        this.exerciseRepository = exerciseRepository;
        this.routineRepository = routineRepository;
    }

    // The global catalog plus the trainer's own exercises, each with how many of their routines use it.
    @Override
    public ExerciseSummaryPage handle(GetExercisesQuery query) {
        var page = exerciseRepository.findAllAvailableTo(query.holderId(), query.search(), query.muscleGroup(),
                query.origin(), query.status(), query.pagination());
        var items = page.items().stream().map(exercise -> summarize(exercise, query.holderId())).toList();
        return new ExerciseSummaryPage(items, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    @Override
    public Optional<ExerciseSummary> handle(GetExerciseByIdQuery query) {
        return exerciseRepository.findAvailableById(query.exerciseId(), query.holderId())
                .map(exercise -> summarize(exercise, query.holderId()));
    }

    @Override
    public List<MuscleGroup> handle(GetMuscleGroupsQuery query) {
        return MuscleGroup.STANDARD;
    }

    private ExerciseSummary summarize(Exercise exercise, String holderId) {
        return new ExerciseSummary(exercise, routineRepository.countByHolderIdAndExerciseId(holderId, exercise.getId()));
    }
}
