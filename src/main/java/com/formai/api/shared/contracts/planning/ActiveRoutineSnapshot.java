package com.formai.api.shared.contracts.planning;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ActiveRoutineSnapshot(UUID clientId,
                                    UUID routineId,
                                    String routineName,
                                    int version,
                                    LocalDate startDate,
                                    Set<String> trainingDays,
                                    List<SessionSnapshot> sessions) { }
