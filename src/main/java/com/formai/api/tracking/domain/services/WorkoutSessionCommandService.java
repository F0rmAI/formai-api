package com.formai.api.tracking.domain.services;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.commands.CorrectSetCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.commands.ScheduleWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.CloseClientOverdueWorkoutSessionsCommand;
import com.formai.api.tracking.domain.model.commands.CloseOverdueWorkoutSessionsCommand;

import java.util.Optional;

public interface WorkoutSessionCommandService {

    Optional<WorkoutSession> handle(ScheduleWorkoutSessionCommand command);

    Optional<WorkoutSession> handle(RecordSetCommand command);

    Optional<WorkoutSession> handle(CorrectSetCommand command);

    Optional<WorkoutSession> handle(FinishWorkoutSessionCommand command);

    void handle(CloseOverdueWorkoutSessionsCommand command);

    void handle(CloseClientOverdueWorkoutSessionsCommand command);
}
