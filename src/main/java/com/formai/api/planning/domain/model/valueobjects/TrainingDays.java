package com.formai.api.planning.domain.model.valueobjects;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// The days of the week the client trains with an assigned routine. A session is scheduled only on
// these days, so rest days do not count as skipped sessions.
public record TrainingDays(Set<DayOfWeek> days) {

    public TrainingDays {
        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException("An assignment needs at least one training day");
        }
        days = Collections.unmodifiableSet(EnumSet.copyOf(days));
    }

    public static TrainingDays everyDay() {
        return new TrainingDays(EnumSet.allOf(DayOfWeek.class));
    }

    // Names as in java.time.DayOfWeek (MONDAY … SUNDAY). No names means every day.
    public static TrainingDays ofNames(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return everyDay();
        }
        var days = EnumSet.noneOf(DayOfWeek.class);
        for (var name : names) {
            try {
                days.add(DayOfWeek.valueOf(name.strip().toUpperCase()));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalArgumentException("Unknown training day: " + name);
            }
        }
        return new TrainingDays(days);
    }

    public boolean includes(DayOfWeek day) {
        return days.contains(day);
    }

    // Monday first, as the enum declares them.
    public List<String> names() {
        return days.stream().sorted().map(DayOfWeek::name).toList();
    }
}
