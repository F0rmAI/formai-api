package com.formai.api.shared.contracts.planning;

import java.util.List;

public record SessionSnapshot(int order, String label, List<PrescribedExerciseSnapshot> exercises) { }
