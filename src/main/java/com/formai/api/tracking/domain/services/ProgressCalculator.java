package com.formai.api.tracking.domain.services;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.entities.SessionExercise;
import com.formai.api.tracking.domain.model.valueobjects.Adherence;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.ComplianceStatus;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseId;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseMetric;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseProgress;
import com.formai.api.tracking.domain.model.valueobjects.ProgressPoint;
import com.formai.api.tracking.domain.model.valueobjects.ProgressReport;
import com.formai.api.tracking.domain.model.valueobjects.ProgressWindow;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProgressCalculator {

    private static final int MIN_POINTS_FOR_A_CHART = 2;

    public ProgressReport report(ClientId clientId, ReportPeriod period, List<WorkoutSession> sessions) {
        var hasData = sessions.stream().anyMatch(WorkoutSession::hasRecords);
        return new ProgressReport(clientId, period, adherence(sessions), sessions.size(),
                count(sessions, ComplianceStatus.COMPLETED), count(sessions, ComplianceStatus.PARTIAL),
                count(sessions, ComplianceStatus.SKIPPED), exerciseMetrics(sessions), hasData);
    }

    public Adherence adherence(List<WorkoutSession> sessions) {
        return Adherence.of(count(sessions, ComplianceStatus.COMPLETED), sessions.size());
    }

    public List<ExerciseMetric> exerciseMetrics(List<WorkoutSession> sessions) {
        Map<ExerciseId, List<SessionExercise>> byExercise = new LinkedHashMap<>();
        chronological(sessions).forEach(session -> session.getExercises().stream()
                .filter(SessionExercise::isRegistered)
                .forEach(exercise -> byExercise.computeIfAbsent(exercise.getExerciseId(), id -> new ArrayList<>())
                        .add(exercise)));
        return byExercise.entrySet().stream()
                .map(entry -> {
                    var first = entry.getValue().getFirst();
                    var last = entry.getValue().getLast();
                    return new ExerciseMetric(entry.getKey(), last.getToPerform().exerciseName(),
                            first.maxLoad(), last.maxLoad(), first.volume(), last.volume());
                })
                .sorted(Comparator.comparing(ExerciseMetric::exerciseName))
                .toList();
    }

    public ExerciseProgress evolution(ExerciseId exerciseId, ProgressWindow window, List<WorkoutSession> sessions) {
        var points = chronological(sessions).stream()
                .flatMap(session -> session.getExercises().stream()
                        .filter(exercise -> exercise.getExerciseId().equals(exerciseId) && exercise.isRegistered())
                        .map(exercise -> new ProgressPoint(session.getScheduledFor(), exercise.maxLoad(),
                                exercise.volume())))
                .toList();
        return new ExerciseProgress(exerciseId, window, points, points.size() >= MIN_POINTS_FOR_A_CHART);
    }

    private static int count(List<WorkoutSession> sessions, ComplianceStatus status) {
        return (int) sessions.stream().filter(session -> session.getStatus() == status).count();
    }

    private static List<WorkoutSession> chronological(List<WorkoutSession> sessions) {
        return sessions.stream().sorted(Comparator.comparing(WorkoutSession::getScheduledFor)).toList();
    }
}
